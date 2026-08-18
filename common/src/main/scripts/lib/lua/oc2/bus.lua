local Channel = require("oc2.channel")
local blob = require("oc2.blob")
local Events = require("oc2.events")
local ports = require("oc2.ports")
local socket = require("oc2.socket")
local time = require("posix.time")

local bus = {}

bus.socketPath = "/run/oc2/bus"
bus.protocolVersion = 1
bus.tokenLength = 32
bus.helloTimeout = 5000
bus.requestTimeout = 35000
bus.maxEventsPerPump = 32
bus.connectAttempts = 5
bus.connectRetryDelay = 200
bus.maxReplySize = Channel.defaultMaxFrame

local rpcPortName = "oc2.rpc.0"
local blobPortName = "oc2.blob.0"
local eventPortName = "oc2.event.0"

bus.rpcPortName = rpcPortName
bus.blobPortName = blobPortName
bus.eventPortName = eventPortName

local roleCode = { rpc = "R", blob = "B", event = "E" }
local defaultRoles = { "rpc", "blob", "event" }

bus.roleCode = roleCode
bus.defaultRoles = defaultRoles

-- Device

local function parameterName(p, index)
  return p.name or ("arg" .. index)
end

local Device = {}
local invokers = {}
local owners = setmetatable({}, {__mode = "k"})

local function busOf(device)
  return owners[device].bus
end

local function methodsOf(device)
  local owner = owners[device]
  if not owner.methods then
    owner.methods = busOf(device):methods(device.deviceId)
  end
  return owner.methods
end

function Device.new(bus_, device)
  owners[device] = { bus = bus_ }
  return setmetatable(device, Device)
end

function Device:invoke(methodName, ...)
  return busOf(self):invoke(self.deviceId, methodName, ...)
end

Device.__index = function(self, key)
  local direct = rawget(Device, key)
  if direct then
    return direct
  end

  local found = false
  for _, method in ipairs(methodsOf(self)) do
    if method.name == key then
      found = true
      break
    end
  end
  if not found then
    return nil
  end

  local invoker = invokers[key]
  if not invoker then
    invoker = function(device, ...)
      return Device.invoke(device, key, ...)
    end
    invokers[key] = invoker
  end
  return invoker
end

Device.__tostring = function(self)
  local out = {}
  local function put(...)
    for _, part in ipairs({...}) do
      out[#out + 1] = part
    end
  end

  for _, method in ipairs(methodsOf(self)) do
    put(method.name, "(")
    if method.parameters then
      for i, p in ipairs(method.parameters) do
        if i > 1 then
          put(", ")
        end
        put(parameterName(p, i))
        if p.type then
          put(": ", p.type)
        end
      end
    end
    put(")")
    if method.returnType then
      put(": ", method.returnType)
    end
    put("\n")

    if method.description then
      put(method.description, "\n")
    end

    if method.parameters then
      for i, p in ipairs(method.parameters) do
        if p.description then
          put("  ", parameterName(p, i), "  ", p.description, "\n")
        end
      end
    end
  end

  return table.concat(out)
end

-- Bus

local function copyDevice(device)
  local copy = {}
  for key, value in pairs(device) do
    if type(value) == "table" then
      local inner = {}
      for innerKey, innerValue in pairs(value) do
        inner[innerKey] = innerValue
      end
      copy[key] = inner
    else
      copy[key] = value
    end
  end
  return copy
end

local function copyDevices(devices)
  local result = {}
  for i, device in ipairs(devices) do
    result[i] = copyDevice(device)
  end
  return result
end

local DeviceBus = {}
DeviceBus.__index = DeviceBus

DeviceBus.null = require("cjson").null

function DeviceBus.new(transport, rpc, payload, events)
  return setmetatable({
    transport = transport, rpc = rpc, payload = payload, events = events,
  }, DeviceBus)
end

function DeviceBus:close()
  self.rpc:close()
  if self.payload then
    self.payload:close()
  end
  if self.events then
    self.events:close()
  end
end

function DeviceBus:flush()
  if self.transport ~= "ports" then
    return
  end
  self.rpc:reset()
  self.payload:reset()
end

local function parseError(result, reason)
  if not result then
    return "unexpected error: " .. tostring(reason or "unknown error")
  elseif result.type == "error" then
    return tostring(result.data or "the host reported an error with no detail")
  else
    return "unexpected message type: " .. tostring(result.type)
  end
end

local function noteGeneration(bus_, result)
  if result.type == "error" and result.gen == nil then
    return -- Daemon error
  end

  local gen = assert(result.gen, "host reply carried no bus generation")
  if gen ~= bus_.generation then
    bus_.generation = gen
    bus_.deviceList = nil
  end
  bus_.generationConfirmed = true
end

local function applyEvent(bus_, event)
  if event.type == "eventsDropped" then
    bus_.deviceList = nil
    bus_.generationConfirmed = false
    return true
  end

  if event.type == "devicesChanged" then
    if event.gen ~= bus_.generation then
      bus_.generation = event.gen
      bus_.deviceList = nil
    end
    bus_.generationConfirmed = true
    return true
  end

  return false
end

function DeviceBus:pumpEvents()
  if not self.events then
    return 0
  end

  local count = 0
  for _ = 1, bus.maxEventsPerPump do
    local event = self.events:poll()
    if not event then
      break -- nothing pending, or a frame we could not parse; either way, stop
    end
    if applyEvent(self, event) then
      count = count + 1
    end
  end
  return count
end

function DeviceBus:waitEvent(timeout, eventType)
  if not self.events then
    return nil, "this session has no event channel"
  end

  while true do
    local event, reason = self.events:wait(timeout)
    if not event then
      return nil, reason
    end

    applyEvent(self, event)
    if not eventType or event.type == eventType then
      return event
    end
  end
end

local function request(bus_, message, expected)
  bus_.rpc:write(message)
  local result, reason = bus_.rpc:read(bus.requestTimeout)
  if not result then
    bus_.rpc:reset()
    if bus_.payload then
      bus_.payload:reset()
    end
    return error("no reply from the host: " .. tostring(reason or "unknown"), 0)
  end
  noteGeneration(bus_, result)
  if result.type == expected then
    return result
  end
  return error(parseError(result, reason), 0)
end

local function cachesLocally(bus_)
  return bus_.transport == "ports"
end

local function rawList(bus_)
  bus_:pumpEvents()

  if cachesLocally(bus_) and bus_.deviceList and bus_.generationConfirmed then
    bus_.generationConfirmed = false
    return bus_.deviceList
  end

  bus_:flush()
  local result = request(bus_, { type = "list" }, "list")
  if cachesLocally(bus_) then
    bus_.deviceList = result.data
  end
  return result.data
end

function DeviceBus:list()
  return copyDevices(rawList(self))
end

local function lookup(bus_, matches)
  for _ = 1, 2 do
    for _, device in ipairs(rawList(bus_)) do
      if matches(device) then
        return Device.new(bus_, copyDevice(device))
      end
    end

    if not bus_.deviceList then
      break -- was not cached, so looking again would return the same thing
    end
    bus_.deviceList = nil
  end
end

function DeviceBus:get(deviceId)
  local device = lookup(self, function(candidate)
    return candidate.deviceId == deviceId
  end)
  if device then
    return device
  end

  return nil, "no device with id [" .. deviceId .. "]"
end

function DeviceBus:find(deviceTypeName)
  local device = lookup(self, function(candidate)
    if candidate.typeNames then
      for _, typeName in ipairs(candidate.typeNames) do
        if typeName == deviceTypeName then
          return true
        end
      end
    end
    return false
  end)
  if device then
    return device
  end

  return nil, "no device of type [" .. deviceTypeName .. "]"
end

function DeviceBus:methods(deviceId)
  self:flush()
  return request(self, { type = "methods", data = deviceId }, "methods").data
end

function DeviceBus:blob(data)
  return blob.wrap(data)
end

function DeviceBus:invoke(deviceId, methodName, ...)
  self:flush()

  local parameters, payload = blob.extract(...)
  local message = { type = "invoke", data = {
    deviceId = deviceId,
    name = methodName,
    parameters = parameters
  }}

  if payload then
    if not self.payload then
      error("this session has no payload channel; open one with the blob role", 0)
    end
    self.payload:write(payload)
    message.blob = { length = #payload, checksum = blob.checksum(payload) }
  end

  local result = request(self, message, "result")
  if result.blob and not self.payload then
    error("the host sent a payload, but this session has no payload channel", 0)
  end
  return blob.resolve(self.payload, result)
end

-- Transports

local function connectPorts()
  local rpcPath = ports.find(rpcPortName)
  if not rpcPath then
    return nil, "no virtio port named " .. rpcPortName .. " was found"
  end
  local blobPath = ports.find(blobPortName)
  if not blobPath then
    return nil, "no virtio port named " .. blobPortName .. " was found"
  end
  local eventPath = ports.find(eventPortName)
  if not eventPath then
    return nil, "no virtio port named " .. eventPortName .. " was found"
  end

  local rpc, rpcReason = Channel.open(rpcPath)
  if not rpc then
    return nil, tostring(rpcReason)
  end

  local payload, payloadReason = blob.open(blobPath)
  if not payload then
    rpc:close()
    return nil, tostring(payloadReason)
  end

  local events, eventReason = Events.open(eventPath)
  if not events then
    rpc:close()
    payload:close()
    return nil, tostring(eventReason)
  end

  return DeviceBus.new("ports", rpc, payload, events)
end

local function connectSocket(path, roles)
  for _, role in ipairs(roles) do
    if not roleCode[role] then
      return nil, "unknown role " .. tostring(role)
    end
  end

  local opened = {}
  local function abandon(reason)
    for _, fd in ipairs(opened) do
      socket.close(fd)
    end
    return nil, reason
  end

  local function dial()
    local fd, reason = socket.connect(path)
    if not fd then
      return nil, reason
    end
    opened[#opened + 1] = fd
    return fd
  end

  local rpcFd, rpcReason = dial()
  if not rpcFd then
    return abandon(tostring(rpcReason))
  end

  local rpc = Channel.fromFd(rpcFd, bus.maxReplySize)
  local sent, sendReason = pcall(function()
    rpc:writeRaw(roleCode.rpc)
    rpc:write({ type = "hello", version = bus.protocolVersion, roles = roles })
  end)
  if not sent then
    return abandon(tostring(sendReason))
  end

  local hello, helloReason = rpc:read(bus.helloTimeout)
  if not hello then
    return abandon("the bus daemon did not answer: " .. tostring(helloReason))
  end
  if hello.type ~= "hello" then
    return abandon("the bus daemon refused the session: " .. parseError(hello))
  end
  if type(hello.token) ~= "string" or #hello.token ~= bus.tokenLength then
    return abandon("the bus daemon sent an unusable session token")
  end

  local payload, events
  for _, role in ipairs(roles) do
    if role ~= "rpc" then
      local fd, reason = dial()
      if not fd then
        return abandon(tostring(reason))
      end
      local attached, attachReason = pcall(function()
        Channel.fromFd(fd):writeRaw(roleCode[role] .. hello.token)
      end)
      if not attached then
        return abandon(tostring(attachReason))
      end
      if role == "blob" then
        payload = blob.fromFd(fd)
      else
        events = Events.fromFd(fd, bus.maxReplySize)
      end
    end
  end

  local self = DeviceBus.new("socket", rpc, payload, events)
  self.generation = hello.gen
  return self
end

local function sleep(milliseconds)
  time.nanosleep({ tv_sec = milliseconds // 1000,
                   tv_nsec = (milliseconds % 1000) * 1000 * 1000 })
end

function bus.connect(options)
  options = options or {}
  local roles = options.roles or defaultRoles

  local path = options.socketPath or os.getenv("OC2_BUS_SOCKET") or bus.socketPath
  local useSocket = path ~= "none" and path ~= ""
  local socketReason, directReason

  for attempt = 1, bus.connectAttempts do
    if useSocket then
      local connected, reason = connectSocket(path, roles)
      if connected then
        return connected
      end
      socketReason = reason
    end

    local direct
    direct, directReason = connectPorts()
    if direct then
      return direct
    end

    if attempt < bus.connectAttempts then
      sleep(bus.connectRetryDelay)
    end
  end

  if socketReason then
    return nil, string.format("could not reach the bus daemon (%s), nor the ports directly (%s)",
                              tostring(socketReason), tostring(directReason))
  end
  return nil, tostring(directReason)
end

bus.DeviceBus = DeviceBus
bus.Device = Device
bus.null = DeviceBus.null

return bus
