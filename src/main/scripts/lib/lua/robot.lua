local robot = assert(require("devices"):find("robot"), "robot device not found")

local time = require("posix.time")

local function sleep(milliseconds)
  time.nanosleep({tv_sec=0,tv_nsec=milliseconds*1000})
end

local function waitForLastAction()
  local id = robot:getLastActionId()

  local result = robot:getActionResult(id)
  while result and result == "INCOMPLETE" do
    sleep(100)
    result = robot:getActionResult(id)
  end

  return result == "SUCCESS"
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

M.move = function(direction)
  M.moveAsync(direction)
  return waitForLastAction()
end

M.moveAsync = function(direction)
  direction = assert(direction, "no direction specified")
  while not robot:move(direction) do
    sleep(100)
  end
end

M.turn = function(direction)
  M.turnAsync(direction)
  return waitForLastAction()
end

M.turnAsync = function(direction)
  direction = assert(direction, "no direction specified")
  while not robot:turn(direction) do
    sleep(100)
  end
end

M.inspect = function(direction)
  direction = assert(direction, "no direction specified")
  return robot:inspect(direction)
end

return M
