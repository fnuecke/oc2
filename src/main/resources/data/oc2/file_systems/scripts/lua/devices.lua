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

local function parseError(result)
  if result.type == "error" then
    return result.data
  else
    return "unexpected message type " .. result.type
  end
end

local function readOne(fd)
  local result, status, errnum = poll.rpoll(fd, 10)
  if result == 1 then
    return unistd.read(fd, 1)
  else
    return result, status, errnum
  end
end

local function readMessage(device)
  local value
  local message = ""
  while true do
    value = readOne(device.fd)
    if not value then
      unistd.sleep(1)
    else
      if value == message_delimiter or value == 0 then
        if message:match("%S") ~= nil then
          local ok, result = pcall(cjson.decode, message)
          if ok then
            return result
          end
        else
          message = ""
        end
      else
        message = message .. value
      end
    end
  end
end

local function writeMessage(device, data)
  local message = cjson.encode(data)
  return unistd.write(device.fd,
      message_delimiter ..
          message ..
          message_delimiter)
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

function DeviceBus:list()
  writeMessage(self, { type = "list" })
  local result = readMessage(self)
  if result.type == "list" then
    return result.data
  else
    return nil, parseError(result)
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
  writeMessage(self, { type = "methods", data = deviceId })
  local result = readMessage(self)
  if result.type == "methods" then
    return result.data
  else
    error(parseError(result))
  end
end

function DeviceBus:invoke(deviceId, methodName, ...)
  writeMessage(self, { type = "invoke", data = {
    deviceId = deviceId,
    name = methodName,
    parameters = { ... }
  } })
  local result = readMessage(self)
  if result.type == "result" then
    return result.data
  else
    error(parseError(result))
  end
end

return DeviceBus:new("/dev/hvc0")
