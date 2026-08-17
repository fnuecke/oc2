local Channel = require("oc2.channel")
local blob = require("oc2.blob")
local Events = require("oc2.events")
local ports = require("oc2.ports")

local requestTimeout = 30000

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

  local function parameterName(p, index)
    return p.name or ("arg" .. index)
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

function Device:new(bus, device)
  owners[device] = { bus = bus }
  return setmetatable(device, self)
end

function Device:invoke(methodName, ...)
  return busOf(self):invoke(self.deviceId, methodName, ...)
end

local DeviceBus = {}
DeviceBus.__index = DeviceBus

DeviceBus.null = require("cjson").null

local function parseError(result, reason)
  if not result then
    return "unexpected error: " .. tostring(reason or "unknown error")
  elseif result.type == "error" then
    return tostring(result.data or "the host reported an error with no detail")
  else
    return "unexpected message type: " .. tostring(result.type)
  end
end

local function noteGeneration(bus, result)
  local gen = assert(result.gen, "host reply carried no bus generation")
  if gen ~= bus.generation then
    bus.generation = gen
    bus.deviceList = nil
  end
  bus.generationConfirmed = true
end

local function applyEvent(bus, event)
  if event.type == "devicesChanged" then
    if event.gen ~= bus.generation then
      bus.generation = event.gen
      bus.deviceList = nil
    end
    bus.generationConfirmed = true
    return true
  end

  return false
end

local maxEventsPerPump = 32

function DeviceBus:pumpEvents()
  local count = 0
  for _ = 1, maxEventsPerPump do
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

function DeviceBus:new(path, blobPath, eventPath)
  local rpc, status = Channel.open(path)
  if not rpc then
    return nil, status
  end

  local payload, payloadStatus = blob.open(blobPath)
  if not payload then
    rpc:close()
    return nil, payloadStatus
  end

  local events, eventStatus = Events.open(eventPath)
  if not events then
    rpc:close()
    payload:close()
    return nil, eventStatus
  end

  return setmetatable({ rpc = rpc, payload = payload, events = events }, self)
end

function DeviceBus:close()
  self.rpc:close()
  self.payload:close()
  self.events:close()
end

function DeviceBus:flush()
  self.rpc:reset()
  self.payload:reset()
end

local function request(bus, message, expected)
  bus.rpc:write(message)
  local result, reason = bus.rpc:read(requestTimeout)
  if not result then
    return error("no reply from the host: " .. tostring(reason or "unknown"), 0)
  end
  noteGeneration(bus, result)
  if result.type == expected then
    return result
  end
  return error(parseError(result, reason), 0)
end

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

local function rawList(bus)
  bus:pumpEvents()

  if bus.deviceList and bus.generationConfirmed then
    bus.generationConfirmed = false
    return bus.deviceList
  end

  bus:flush()
  local result = request(bus, { type = "list" }, "list")
  bus.deviceList = result.data
  return result.data
end

function DeviceBus:list()
  return copyDevices(rawList(self))
end

local function lookup(bus, matches)
  for _ = 1, 2 do
    for _, device in ipairs(rawList(bus)) do
      if matches(device) then
        return Device:new(bus, copyDevice(device))
      end
    end

    if not bus.deviceList then
      break -- was not cached, so looking again would return the same thing
    end
    bus.deviceList = nil
  end
end

function DeviceBus:get(deviceId)
  local device, status = lookup(self, function(candidate)
    return candidate.deviceId == deviceId
  end)
  if device then
    return device
  end

  return nil, status or ("no device with id [" .. deviceId .. "]")
end

--- Finds a device by type name. Returns nil plus a reason if there is none; raises only if
--- the bus itself failed.
function DeviceBus:find(deviceTypeName)
  local device, status = lookup(self, function(candidate)
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

  return nil, status or ("no device of type [" .. deviceTypeName .. "]")
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
    self.payload:write(payload)
    message.blob = { length = #payload, checksum = blob.checksum(payload) }
  end

  return blob.resolve(self.payload, request(self, message, "result"))
end

local rpc_port = "oc2.rpc.0"
local blob_port = "oc2.blob.0"
local event_port = "oc2.event.0"

local bus, reason = DeviceBus:new(
  assert(ports.find(rpc_port), "no virtio port named " .. rpc_port .. " was found"),
  assert(ports.find(blob_port), "no virtio port named " .. blob_port .. " was found"),
  assert(ports.find(event_port), "no virtio port named " .. event_port .. " was found"))

return assert(bus, "could not open the device bus: " .. tostring(reason))
