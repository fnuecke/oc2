local robot = assert(require("devices"):find("robot"), "robot device not found")

local time = require("posix.time")

local function sleep(milliseconds)
  local total = math.floor(milliseconds)
  time.nanosleep({tv_sec=total//1000,tv_nsec=(total%1000)*1000000})
end

local pollInterval = 100
local defaultTimeout = 30000

local function deadlineFrom(timeout)
  return os.time() + (timeout or defaultTimeout) / 1000
end

local function waitForLastAction(timeout)
  local id = robot:getLastActionId()
  local deadline = deadlineFrom(timeout)

  local result = robot:getActionResult(id)
  while result and result == "INCOMPLETE" do
    if os.time() >= deadline then
      return false
    end
    sleep(pollInterval)
    result = robot:getActionResult(id)
  end

  return result == "SUCCESS"
end

local function queueAction(action, direction, timeout)
  direction = assert(direction, "no direction specified")
  local deadline = deadlineFrom(timeout)
  while not action(robot, direction) do
    if os.time() >= deadline then
      return false
    end
    sleep(pollInterval)
  end
  return true
end

local M = {}

M.direction = {
  forward = "forward",
  backward = "backward",
  upward = "upward",
  downward = "downward",
  left = "left",
  right = "right"
}

M.energy = function()
  return robot:getEnergyStored()
end

M.capacity = function()
  return robot:getEnergyCapacity()
end

M.slot = function(value)
  if value then
    robot:setSelectedSlot(value)
  end
  return robot:getSelectedSlot()
end

M.stack = function(slot)
  return robot:getStackInSlot(slot or M.slot())
end

M.move = function(direction, timeout)
  if not M.moveAsync(direction, timeout) then
    return false
  end
  return waitForLastAction(timeout)
end

M.moveAsync = function(direction, timeout)
  return queueAction(robot.move, direction, timeout)
end

M.turn = function(direction, timeout)
  if not M.turnAsync(direction, timeout) then
    return false
  end
  return waitForLastAction(timeout)
end

M.turnAsync = function(direction, timeout)
  return queueAction(robot.turn, direction, timeout)
end

return M
