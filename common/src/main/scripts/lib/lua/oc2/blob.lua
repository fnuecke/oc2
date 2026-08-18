local fcntl = require("posix.fcntl")
local unistd = require("posix.unistd")
local poll = require("posix.poll")
local json_null = require("cjson").null
local Channel = require("oc2.channel")

local blob = {}

local chunkSize = 32 * 1024
local readTimeout = 5000
local maxDepth = 32

blob.key = "$blob"
blob.maxOutbound = 512 * 1024
blob.maxInbound = blob.maxOutbound
blob.chunkSize = chunkSize
blob.readTimeout = readTimeout
blob.maxDepth = maxDepth
blob.marker = {}

function blob.wrap(data)
  if type(data) ~= "string" then
    error("a binary payload must be a string, got " .. type(data), 2)
  end
  return setmetatable({ data = data }, blob.marker)
end

function blob.checksum(data)
  local sum = 0
  local length = #data
  local i = 1
  while i <= length do
    local word
    if i + 3 <= length then
      word = string.unpack("<I4", data, i)
    else
      word = string.unpack("<I4", data:sub(i) .. string.rep("\0", 4 - (length - i + 1)))
    end
    sum = (((sum << 1) | (sum >> 31)) + word) & 0xFFFFFFFF
    i = i + 4
  end
  if sum >= 0x80000000 then
    sum = sum - 0x100000000
  end
  return sum
end

function blob.extract(...)
  local packed = table.pack(...)
  local parameters = {}
  local payload
  for i = 1, packed.n do
    local value = packed[i]
    if getmetatable(value) == blob.marker then
      if payload then
        error("a call may carry at most one binary payload", 2)
      end
      payload = value.data
      parameters[i] = { [blob.key] = true }
    elseif value == nil then
      parameters[i] = json_null
    else
      parameters[i] = value
    end
  end
  return parameters, payload
end

function blob.substitute(value, payload, depth)
  if type(value) ~= "table" then
    return value
  end

  depth = depth or 0
  if depth > maxDepth then
    error("host reply is nested too deeply", 0)
  end

  if value[blob.key] then
    return payload
  end

  for key, item in pairs(value) do
    value[key] = blob.substitute(item, payload, depth + 1)
  end
  return value
end

local Payload = {}
Payload.__index = Payload

function blob.fromFd(fd)
  return setmetatable({ fd = fd }, Payload)
end

function blob.open(path)
  local fd, status = fcntl.open(path, fcntl.O_RDWR | fcntl.O_CLOEXEC | fcntl.O_NONBLOCK)
  if not fd then
    return nil, status
  end
  return blob.fromFd(fd)
end

function Payload:close()
  if self.fd then
    unistd.close(self.fd)
    self.fd = nil -- closing twice would close whatever reused the number
  end
end

function Payload:reset()
  repeat
    local ready = poll.rpoll(self.fd, 0)
    if ready == 1 then
      local data = unistd.read(self.fd, chunkSize)
      if not data or #data == 0 then
        break
      end
    end
  until ready ~= 1
end

function Payload:write(data)
  if #data > blob.maxOutbound then
    error(string.format("binary payload of %d bytes exceeds the host limit of %d bytes",
                        #data, blob.maxOutbound), 2)
  end

  Channel.writeAll(self.fd, data)
end

function Payload:read(length, timeout)
  return Channel.readExactly(self.fd, length, timeout or readTimeout)
end

function blob.resolve(channel, result)
  local reference = result.blob
  if not reference then
    return result.data
  end

  if reference.length < 0 or reference.length > blob.maxInbound then
    error("host announced an implausible payload size: " .. tostring(reference.length), 0)
  end

  local data, reason = channel:read(reference.length)
  if not data then
    channel:reset()
    error("could not read binary payload: " .. tostring(reason), 0)
  end
  if blob.checksum(data) ~= reference.checksum then
    channel:reset()
    error("binary payload failed its checksum; the data channel is corrupt or out of sync", 0)
  end

  return blob.substitute(result.data, data)
end

return blob
