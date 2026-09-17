-- Addressed data-frames over a serial port.
--
-- Frame format: 01h  to  from  length  payload...  checksum
--
-- Usage:
--
--   local serial = require("oc2.serial")
--   local line = assert(serial.open())
--   line:send(7, "status?") -- to address 7
--   local from, data = line:receive(5000)

local Channel = require("oc2.channel")
local clock = require("oc2.clock")
local fcntl = require("posix.fcntl")
local poll = require("posix.poll")
local unistd = require("posix.unistd")

local serial = {}

local SOH = 0x01
local MAX_PAYLOAD = 200
local BROADCAST = 255
local READ_CHUNK = 256

local Line = {}
Line.__index = Line

local function checksum(bytes)
  local sum = 0
  for i = 1, #bytes do
    sum = (sum + bytes:byte(i)) % 256
  end
  return sum
end

local function baseAddress(device)
  local file = io.open("/sys/class/tty/" .. device:match("[^/]+$") .. "/iomem_base")
  if not file then
    return nil
  end
  local base = tonumber(file:read("l"))
  file:close()
  return base
end

local function cardSettings(device)
  local devices = require("devices")

  local base = baseAddress(device)
  if not base then
    return nil, device .. " is not a serial port"
  end

  for _, entry in ipairs(devices:list()) do
    for _, typeName in ipairs(entry.typeNames or {}) do
      if typeName == "serial" then
        local card = devices:get(entry.deviceId)
        if card:getBaseAddress() == base then
          return { card = card, address = card:getAddress() }
        end
      end
    end
  end

  return nil, "no serial interface card behind " .. device
end

function serial.open(device, baud)
  device = device or "/dev/ttyS1"

  local settings, reason = cardSettings(device)
  if not settings then
    return nil, reason
  end

  local speed = baud and tostring(baud) or ""
  os.execute(("stty -F %s %s raw -echo -crtscts 2>/dev/null"):format(device, speed))

  local fd, openReason = fcntl.open(device, fcntl.O_RDWR | fcntl.O_CLOEXEC | fcntl.O_NONBLOCK)
  if not fd then
    return nil, openReason
  end

  return setmetatable({
    fd = fd,
    card = settings.card,
    address = settings.address,
    buffer = "",
  }, Line)
end

function Line:close()
  if self.fd then
    unistd.close(self.fd)
    self.fd = nil
  end
end

function serial.encode(to, from, data)
  assert(#data <= MAX_PAYLOAD, "payload does not fit a frame")

  local header = string.char(to % 256, from % 256, #data)
  return string.char(SOH) .. header .. data .. string.char(checksum(header .. data))
end

function serial.decode(buffer, address)
  local marker = string.char(SOH)
  local pending
  local start = buffer:find(marker, 1, true)
  while start do
    local length = buffer:byte(start + 3)
    local last = length and start + 4 + length
    if not length then
      pending = pending or start
      break
    elseif length <= MAX_PAYLOAD then
      if #buffer < last then
        pending = pending or start
      elseif buffer:byte(last) == checksum(buffer:sub(start + 1, last - 1)) then
        local to = buffer:byte(start + 1)
        if to == address or to == BROADCAST then
          return { from = buffer:byte(start + 2), to = to, data = buffer:sub(start + 4, last - 1) }, buffer:sub(last + 1)
        end
        buffer = buffer:sub(last + 1)
        pending = nil
        start = 0
      end
    end
    start = buffer:find(marker, start + 1, true)
  end
  return nil, pending and buffer:sub(pending) or ""
end

function Line:send(to, data)
  Channel.writeAll(self.fd, serial.encode(to, self.address, data))
end

function Line:broadcast(data)
  self:send(BROADCAST, data)
end

function Line:baudRate()
  return self.card:getBaudRate()
end

function Line:collisions()
  return self.card:getTxErrorCount()
end

function Line:receive(timeout)
  local deadline = timeout and clock.deadline(timeout)

  while true do
    local frame = self:parse()
    if frame then
      return frame.from, frame.data, frame.to
    end

    if not self:fill(deadline) then
      return nil
    end
  end
end

function Line:fill(deadline)
  while true do
    local wait = deadline and clock.remaining(deadline) or -1
    if deadline and wait <= 0 then
      return false
    end

    local ready, reason, number = poll.rpoll(self.fd, wait)
    if ready == nil then
      if not Channel.isRetryable(number) then
        error("could not wait on the line: " .. tostring(reason), 0)
      end
    elseif ready == 0 then
      return false
    else
      local chunk, readReason, readNumber = unistd.read(self.fd, READ_CHUNK)
      if chunk and #chunk > 0 then
        self.buffer = self.buffer .. chunk
        return true
      elseif chunk then
        return false
      elseif not Channel.isRetryable(readNumber) then
        error("could not read the line: " .. tostring(readReason), 0)
      end
    end
  end
end

function Line:parse()
  local frame, rest = serial.decode(self.buffer, self.address)
  self.buffer = rest
  return frame
end

serial.BROADCAST = BROADCAST
serial.MAX_PAYLOAD = MAX_PAYLOAD

return serial
