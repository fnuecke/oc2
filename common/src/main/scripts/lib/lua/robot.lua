local bus = require("devices")
local clock = require("oc2.clock")
local robot = assert(bus:find("robot"), "robot device not found")

local pollInterval = 1000
local defaultTimeout = 30000
local actionCompleted = "robotActionCompleted"

local function deadlineFrom(timeout)
  return clock.deadline(timeout or defaultTimeout)
end

local function completedActionResult(event, id)
  if event and event.data.actionId == id then
    return event.data.result
  end
end

local function waitForLastAction(timeout)
  local id = robot:getLastActionId()
  local deadline = deadlineFrom(timeout)

  local result = robot:getActionResult(id)
  while result and result == "INCOMPLETE" do
    if clock.expired(deadline) then
      return false
    end
    result = completedActionResult(bus:waitEvent(pollInterval, actionCompleted), id)
        or robot:getActionResult(id)
  end

  return result == "SUCCESS"
end

local function queueAction(action, direction, timeout)
  direction = assert(direction, "no direction specified")
  local deadline = deadlineFrom(timeout)
  while not action(robot, direction) do
    if clock.expired(deadline) then
      return false
    end
    bus:waitEvent(pollInterval, actionCompleted) -- a slot frees up when an action completes
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

M.side = {
  front = "front",
  up = "up",
  down = "down"
}

M.detect = function(side)
  side = assert(side, "no side specified")
  return robot:detect(side)
end

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
