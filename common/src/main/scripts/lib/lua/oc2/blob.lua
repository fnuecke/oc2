local fcntl = require("posix.fcntl")
local unistd = require("posix.unistd")
local poll = require("posix.poll")
local json_null = require("cjson").null

local blob = {}
local readSize = 32 * 1024

local readTimeout = 5000

blob.key = "$blob"
blob.maxOutbound = 512 * 1024
blob.maxInbound = blob.maxOutbound
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

function blob.substitute(value, payload)
  if type(value) ~= "table" then
    return value
  end

  if value[blob.key] then
    return payload
  end

  for key, item in pairs(value) do
    value[key] = blob.substitute(item, payload)
  end
  return value
end

local Payload = {}
Payload.__index = Payload

function blob.open(path)
  local fd, status = fcntl.open(path, fcntl.O_RDWR | fcntl.O_CLOEXEC)
  if not fd then
    return nil, status
  end
  return setmetatable({ fd = fd }, Payload)
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
    if ready == 1 and not unistd.read(self.fd, readSize) then
      break -- the port is gone; looping on a failing read would spin forever
    end
  until ready ~= 1
end

function Payload:write(data)
  if #data > blob.maxOutbound then
    error(string.format("binary payload of %d bytes exceeds the host limit of %d bytes",
                        #data, blob.maxOutbound), 2)
  end

  local offset = 1
  while offset <= #data do
    local written, reason = unistd.write(self.fd, data:sub(offset, offset + readSize - 1))
    if not written or written <= 0 then
      error("could not write binary payload: " .. tostring(reason), 0)
    end
    offset = offset + written
  end
end

function Payload:read(length)
  local parts = {}
  local remaining = length
  while remaining > 0 do
    local ready, status, errnum = poll.rpoll(self.fd, readTimeout)
    if not ready then
      return nil, status, errnum
    elseif ready == 0 then
      return nil, "timed out waiting for the rest of the payload"
    end

    local chunk, reason = unistd.read(self.fd, math.min(remaining, readSize))
    if not chunk or #chunk == 0 then
      return nil, reason or "end of file"
    end

    parts[#parts + 1] = chunk
    remaining = remaining - #chunk
  end
  return table.concat(parts)
end

function blob.resolve(channel, result)
  local reference = result and result.blob
  if type(reference) ~= "table" or not reference.length then
    return result and result.data
  end

  if not channel then
    error("host sent a binary payload but no data channel was found")
  end
  if reference.length < 0 or reference.length > blob.maxInbound then
    error("host announced an implausible payload size: " .. tostring(reference.length))
  end

  local data, reason = channel:read(reference.length)
  if not data then
    channel:reset()
    error("could not read binary payload: " .. tostring(reason))
  end
  if blob.checksum(data) ~= reference.checksum then
    channel:reset()
    error("binary payload failed its checksum; the data channel is corrupt or out of sync")
  end

  return blob.substitute(result.data, data)
end

return blob
