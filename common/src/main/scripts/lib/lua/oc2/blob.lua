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

local crcTable = {}
for i = 0, 255 do
  local c = i
  for _ = 1, 8 do
    c = (c & 1) ~= 0 and (0xEDB88320 ~ (c >> 1)) or (c >> 1)
  end
  crcTable[i] = c
end

function blob.checksum(data)
  local crc = 0xFFFFFFFF
  local byte = string.byte
  local length = #data
  local i = 1
  while i + 7 <= length do
    local b1, b2, b3, b4, b5, b6, b7, b8 = byte(data, i, i + 7)
    crc = crcTable[(crc ~ b1) & 0xFF] ~ (crc >> 8)
    crc = crcTable[(crc ~ b2) & 0xFF] ~ (crc >> 8)
    crc = crcTable[(crc ~ b3) & 0xFF] ~ (crc >> 8)
    crc = crcTable[(crc ~ b4) & 0xFF] ~ (crc >> 8)
    crc = crcTable[(crc ~ b5) & 0xFF] ~ (crc >> 8)
    crc = crcTable[(crc ~ b6) & 0xFF] ~ (crc >> 8)
    crc = crcTable[(crc ~ b7) & 0xFF] ~ (crc >> 8)
    crc = crcTable[(crc ~ b8) & 0xFF] ~ (crc >> 8)
    i = i + 8
  end
  while i <= length do
    crc = crcTable[(crc ~ byte(data, i)) & 0xFF] ~ (crc >> 8)
    i = i + 1
  end
  return crc ~ 0xFFFFFFFF
end

local function encode(value, payloads, indices, depth)
  if getmetatable(value) == blob.marker then
    local index = indices[value]
    if not index then
      payloads[#payloads + 1] = value.data
      index = #payloads - 1
      indices[value] = index
    end
    return { [blob.key] = index }
  end
  if type(value) ~= "table" then
    return value
  end
  if depth > maxDepth then
    error("parameters are nested too deeply", 0)
  end

  -- Copied only when a payload is found, the caller's tables are never modified.
  local copy
  for key, item in pairs(value) do
    local encoded = encode(item, payloads, indices, depth + 1)
    if encoded ~= item then
      if not copy then
        copy = setmetatable({}, getmetatable(value))
        for k, v in pairs(value) do
          copy[k] = v
        end
      end
      copy[key] = encoded
    end
  end
  return copy or value
end

function blob.extract(...)
  local packed = table.pack(...)
  local parameters = {}
  local payloads = {}
  local indices = {}
  for i = 1, packed.n do
    local value = packed[i]
    if value == nil then
      parameters[i] = json_null
    else
      parameters[i] = encode(value, payloads, indices, 0)
    end
  end
  return parameters, payloads
end

function blob.substitute(value, payloads, depth)
  if type(value) ~= "table" then
    return value
  end

  depth = depth or 0
  if depth > maxDepth then
    error("host reply is nested too deeply", 0)
  end

  local index = value[blob.key]
  if index ~= nil then
    return payloads[index + 1]
  end

  for key, item in pairs(value) do
    value[key] = blob.substitute(item, payloads, depth + 1)
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

function blob.length(references)
  local length = 0
  for _, reference in ipairs(references) do
    if reference.length < 0 then
      return -1
    end
    length = length + reference.length
  end
  return length
end

function blob.resolve(channel, result)
  local references = result.blobs
  if not references or #references == 0 then
    return result.data
  end

  local length = blob.length(references)
  if length < 0 or length > blob.maxInbound then
    error("host announced an implausible payload size: " .. tostring(length), 0)
  end

  local payloads = {}
  for i, reference in ipairs(references) do
    local data, reason = channel:read(reference.length)
    if not data then
      channel:reset()
      error("could not read binary payload: " .. tostring(reason), 0)
    end
    if blob.checksum(data) ~= reference.checksum then
      channel:reset()
      error("binary payload failed its checksum; the data channel is corrupt or out of sync", 0)
    end
    payloads[i] = data
  end

  return blob.substitute(result.data, payloads)
end

return blob
