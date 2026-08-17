local time = require("posix.time")

local clock = {}

function clock.ms()
  local now = time.clock_gettime(time.CLOCK_MONOTONIC)
  return now.tv_sec * 1000 + now.tv_nsec // 1000000
end

function clock.deadline(timeout)
  return clock.ms() + timeout
end

function clock.remaining(deadline)
  local left = deadline - clock.ms()
  return left > 0 and left or 0
end

function clock.expired(deadline)
  return deadline - clock.ms() <= 0
end

return clock
