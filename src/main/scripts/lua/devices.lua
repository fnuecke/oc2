local fcntl = require("posix.fcntl")
local unistd = require("posix.unistd")
local poll = require("posix.poll")
local cjson = require("cjson").new()

local Device = {}
Device.__index = function(_, key)
  return rawget(Device, key) or function(self, ...)
    return Device.invoke(self, key, ...)
  end
end
Device.__tostring = function(self)
  local doc = ""

  if not rawget(self, "methods") then
    self.methods = self.bus:methods(self.deviceId)
  end

  for _, method in ipairs(self.methods) do
    if method.description then
      doc = doc .. method.description .. "\n"
    end

    if method.parameters then
      local i = 1
      for _, p in ipairs(method.parameters) do
        if p.description then
          doc = doc .. "  "
          if p.name then
            doc = doc .. p.name
          else
            doc = doc .. "arg" .. i
          end
          doc = doc .. "  " .. p.description .. "\n"
        end

        i = i + 1
      end
    end

    doc = doc .. method.name .. "("
    if method.parameters then
      local i = 1
      for _, p in ipairs(method.parameters) do
        if i > 1 then
          doc = doc .. ", "
        end
        if p.name then
          doc = doc .. p.name
        else
          doc = doc .. "arg" .. i
        end
        doc = doc .. ": " .. p.type
        i = i + 1
      end
    end
    doc = doc .. "): " .. method.returnType .. "\n"
  end

  return doc
end

function Device:new(bus, device)
  device.bus = bus
  return setmetatable(device, self)
end

function Device:invoke(methodName, ...)
  return self.bus:invoke(self.deviceId, methodName, ...)
end

local DeviceBus = {}
DeviceBus.__index = DeviceBus

local message_delimiter = string.char(0)

local function parseError(result, reason)
  if result and result.type == "error" then
    return result.data
  elseif result then
    return "unexpected message type: " .. result.type
  else
    return "unexpected error: " .. (reason or "unknown error")
  end
end

local function skipInput(bus)
  repeat
    local result, status, errnum = poll.rpoll(bus.fd, 0)
    if result == 1 then
      unistd.read(bus.fd, 1024)
    end
  until result ~= 1
end

local function fillBuffer(bus)
  local result, status, errnum = poll.rpoll(bus.fd, -1)
  if result == nil then
    return result, status, errnum
  elseif result == 0 then
    return nil, "timeout"
  else
    bus.buffer = unistd.read(bus.fd, 1024)
    bus.bufferLen = string.len(bus.buffer)
    bus.bufferPos = 1
    return true
  end
end

local function clearBuffer(bus)
    bus.buffer = nil
end

local function readOne(bus)
  if not bus.buffer then
    local result, status = fillBuffer(bus)
    if not result then
      return result, status
    end
  end

  local result = bus.buffer:byte(bus.bufferPos)
  if bus.bufferPos >= bus.bufferLen then
    bus.buffer = nil
  else
    bus.bufferPos = bus.bufferPos + 1
  end

  return result
end

local function readMessage(bus)
  local value
  local message = ""
  while true do
    value, reason = readOne(bus)
    if value == nil then -- error
      return value, reason
    else
      if value == 0 then
        if message:match("%S") ~= nil then
          local ok, result = pcall(cjson.decode, message)
          if ok then
            return result
          else
            return nil, result
          end
        else
          message = ""
        end
      else
        message = message .. string.char(value)
      end
    end
  end
end

local function writeMessage(bus, data)
  local message = cjson.encode(data)
  return unistd.write(bus.fd, message_delimiter .. message .. message_delimiter)
end

function DeviceBus:new(path)
  local fd, status = fcntl.open(path, fcntl.O_RDWR)
  if not fd then
    return nil, status
  end

  os.execute("stty -F " .. path .. " raw -echo")

  return setmetatable({ fd = fd }, self)
end

function DeviceBus:close()
  unistd.close(self.fd)
end

function DeviceBus:flush()
  clearBuffer(self)
  skipInput(self)
end

function DeviceBus:list()
  self:flush()
  writeMessage(self, { type = "list" })
  local result, reason = readMessage(self)
  if result and result.type == "list" then
    return result.data
  else
    return error(parseError(result))
  end
end

function DeviceBus:get(deviceId)
  local devices, status = self:list()
  if not devices then
    return nil, status
  end

  for _, device in ipairs(devices) do
    if device.deviceId == deviceId then
      return Device:new(self, device)
    end
  end

  return nil, "no device with id [" .. deviceId .. "]"
end

function DeviceBus:find(deviceTypeName)
  local devices, status = self:list()
  if not devices then
    return nil, status
  end

  for _, device in ipairs(devices) do
    if device.typeNames then
      for _, typeName in ipairs(device.typeNames) do
        if typeName == deviceTypeName then
          return Device:new(self, device)
        end
      end
    end
  end

  return nil, "no device of type [" .. deviceTypeName .. "]"
end

function DeviceBus:methods(deviceId)
  self:flush()
  writeMessage(self, { type = "methods", data = deviceId })
  local result, reason = readMessage(self)
  if result and result.type == "methods" then
    return result.data
  else
    error(parseError(result, reason))
  end
end

function DeviceBus:invoke(deviceId, methodName, ...)
  self:flush()
  writeMessage(self, { type = "invoke", data = {
    deviceId = deviceId,
    name = methodName,
    parameters = { ... }
  }})
  local result, reason = readMessage(self)
  if result and result.type == "result" then
    return result.data
  else
    error(parseError(result, reason))
  end
end

return DeviceBus:new("/dev/hvc0")
