local fcntl = require("posix.fcntl")
local unistd = require("posix.unistd")
local poll = require("posix.poll")
local cjson = require("cjson").new()

if cjson.encode_empty_table_as_object then
  cjson.encode_empty_table_as_object(false)
end

local Channel = {}
Channel.__index = Channel

local delimiter = "\0"
local readSize = 4096

function Channel.open(path, readOnly)
  local flags = (readOnly and fcntl.O_RDONLY or fcntl.O_RDWR) | fcntl.O_CLOEXEC
  local fd, status = fcntl.open(path, flags)
  if not fd then
    return nil, status
  end
  return setmetatable({ fd = fd, parts = {} }, Channel)
end

function Channel:close()
  if self.fd then
    unistd.close(self.fd)
    self.fd = nil -- closing twice would close whatever reused the number
  end
  self.buffer = nil
end

function Channel:reset()
  self.buffer = nil
  self.parts = {}
  repeat
    local ready = poll.rpoll(self.fd, 0)
    if ready == 1 and not unistd.read(self.fd, readSize) then
      break -- the port is gone; looping on a failing read would spin forever
    end
  until ready ~= 1
end

function Channel:fill(timeout)
  local result, status, errnum = poll.rpoll(self.fd, timeout or -1)
  if result == nil then
    return result, status, errnum
  elseif result == 0 then
    return nil, "timeout"
  else
    local data, reason = unistd.read(self.fd, readSize)
    if not data or #data == 0 then
      return nil, reason or "end of file"
    end
    self.buffer = data
    self.bufferPos = 1
    return true
  end
end

function Channel:read(timeout)
  local parts = self.parts
  while true do
    if not self.buffer then
      local result, status = self:fill(timeout)
      if not result then
        return result, status
      end
    end

    while #parts == 0 and self.bufferPos <= #self.buffer
        and self.buffer:byte(self.bufferPos) == 0 do
      self.bufferPos = self.bufferPos + 1
    end

    if self.bufferPos > #self.buffer then
      self.buffer = nil
    else
      local stop = self.buffer:find(delimiter, self.bufferPos, true)
      if stop then
        parts[#parts + 1] = self.buffer:sub(self.bufferPos, stop - 1)
        self.bufferPos = stop + 1
        if self.bufferPos > #self.buffer then
          self.buffer = nil
        end

        local frame = table.concat(parts)
        self.parts = {}
        parts = self.parts

        local ok, result = pcall(cjson.decode, frame)
        if ok then
          return result
        end
      else
        parts[#parts + 1] = self.buffer:sub(self.bufferPos)
        self.buffer = nil
      end
    end
  end
end

function Channel:write(data)
  local message = delimiter .. cjson.encode(data) .. delimiter
  local offset = 1
  while offset <= #message do
    local written, reason = unistd.write(self.fd, message:sub(offset, offset + 65535))
    if not written or written <= 0 then
      error("could not write message: " .. tostring(reason), 0)
    end
    offset = offset + written
  end
end

return Channel
