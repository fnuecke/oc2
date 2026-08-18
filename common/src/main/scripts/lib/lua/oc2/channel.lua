local errno = require("posix.errno")
local fcntl = require("posix.fcntl")
local unistd = require("posix.unistd")
local poll = require("posix.poll")
local cjson = require("cjson").new()
local clock = require("oc2.clock")

if cjson.encode_empty_table_as_object then
  cjson.encode_empty_table_as_object(false)
end

local Channel = {}
Channel.__index = Channel

local delimiter = "\0"
local readSize = 4096
local writeTimeout = 10000
local defaultMaxFrame = 256 * 1024

Channel.delimiter = delimiter
Channel.readSize = readSize
Channel.writeTimeout = writeTimeout
Channel.defaultMaxFrame = defaultMaxFrame

function Channel.isRetryable(number)
  return number == errno.EAGAIN or number == errno.EWOULDBLOCK or number == errno.EINTR
end

local isRetryable = Channel.isRetryable

function Channel.frame(data)
  return delimiter .. cjson.encode(data) .. delimiter
end

function Channel.fromFd(fd, maxFrame)
  return setmetatable({ fd = fd, parts = {}, partsSize = 0,
                        maxFrame = maxFrame or defaultMaxFrame }, Channel)
end

function Channel.open(path, readOnly)
  local flags = (readOnly and fcntl.O_RDONLY or fcntl.O_RDWR)
      | fcntl.O_CLOEXEC | fcntl.O_NONBLOCK
  local fd, status = fcntl.open(path, flags)
  if not fd then
    return nil, status
  end
  return Channel.fromFd(fd)
end

function Channel:close()
  if self.fd then
    unistd.close(self.fd)
    self.fd = nil -- closing twice would close whatever reused the number
  end
  self.buffer = nil
  self.parts = {}
  self.partsSize = 0
end

function Channel:reset()
  self.buffer = nil
  self.parts = {}
  self.partsSize = 0
  repeat
    local ready = poll.rpoll(self.fd, 0)
    if ready == 1 then
      local data = unistd.read(self.fd, readSize)
      if not data or #data == 0 then
        break
      end
    end
  until ready ~= 1
end

function Channel:fill(timeout)
  local deadline = timeout and timeout >= 0 and clock.deadline(timeout)
  while true do
    local ready, status, number = poll.rpoll(self.fd, deadline and clock.remaining(deadline) or -1)
    if ready == nil then
      if not isRetryable(number) then
        return nil, status, number
      end
    elseif ready == 0 then
      return nil, "timeout"
    else
      local data, reason, readNumber = unistd.read(self.fd, readSize)
      if data and #data > 0 then
        self.buffer = data
        self.bufferPos = 1
        return true
      elseif data then
        return nil, "end of file"
      elseif not isRetryable(readNumber) then
        return nil, reason
      end
    end

    if deadline and clock.expired(deadline) then
      return nil, "timeout"
    end
  end
end

function Channel:read(timeout)
  local deadline = timeout and timeout >= 0 and clock.deadline(timeout)
  local parts = self.parts
  while true do
    if not self.buffer then
      local result, status = self:fill(deadline and clock.remaining(deadline))
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
        self.partsSize = 0
        parts = self.parts

        local ok, result = pcall(cjson.decode, frame)
        if ok and type(result) == "table" then -- can get one broken message after reset
          return result
        end
      else
        parts[#parts + 1] = self.buffer:sub(self.bufferPos)
        self.partsSize = self.partsSize + #self.buffer - self.bufferPos + 1
        self.buffer = nil

        if self.partsSize > self.maxFrame then
          self.parts = {}
          self.partsSize = 0
          return nil, "frame exceeds " .. self.maxFrame .. " bytes"
        end
      end
    end
  end
end

function Channel:write(data)
  Channel.writeAll(self.fd, Channel.frame(data))
end

function Channel:writeRaw(data)
  Channel.writeAll(self.fd, data)
end

function Channel.readExactly(fd, length, timeout)
  assert(math.type(fd) == "integer", "readExactly takes a descriptor, not a channel")
  assert(timeout and timeout >= 0, "readExactly needs a bounded timeout")

  local deadline = clock.deadline(timeout)
  local parts = {}
  local remaining = length
  while remaining > 0 do
    local ready, status, number = poll.rpoll(fd, clock.remaining(deadline))
    if ready == nil then
      if not isRetryable(number) then
        return nil, status, number
      end
    elseif ready == 0 then
      return nil, "timeout"
    else
      local chunk, reason, readNumber = unistd.read(fd, math.min(remaining, readSize))
      if chunk and #chunk > 0 then
        parts[#parts + 1] = chunk
        remaining = remaining - #chunk
      elseif chunk then
        return nil, "end of file"
      elseif not isRetryable(readNumber) then
        return nil, reason
      end
    end

    if remaining > 0 and clock.expired(deadline) then
      return nil, "timeout"
    end
  end
  return table.concat(parts)
end

function Channel.writeAll(fd, data)
  local offset = 1
  local length = #data
  local fds = {[fd] = {events = {OUT = true}}}
  while offset <= length do
    local chunk = offset == 1 and data or data:sub(offset)
    local written, reason, number = unistd.write(fd, chunk)
    if written and written > 0 then
      offset = offset + written
    elseif written == nil and not isRetryable(number) then
      error("could not write to the channel: " .. tostring(reason), 0)
    else
      local ready = poll.poll(fds, writeTimeout)
      if not ready or ready == 0 then
        error("timed out writing to the channel", 0)
      end
      local revents = fds[fd].revents
      if revents and (revents.ERR or revents.HUP or revents.NVAL) then
        error("the far end of the channel went away", 0)
      end
    end
  end
end

return Channel
