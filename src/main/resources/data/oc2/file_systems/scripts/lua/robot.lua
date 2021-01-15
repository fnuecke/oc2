local robot = assert(require("devices"):find("robot"), "robot device not found")

local time = require("posix.time")

local function sleep(milliseconds)
  time.nanosleep({tv_sec=0,tv_nsec=milliseconds*1000})
end

local M = {}

local directions = {
  forward = "FORWARD",
  backward = "BACKWARD",
  up = "UP",
  down = "DOWN",
  left = "LEFT",
  right = "RIGHT"
}

local directionAliases = {
  [directions.forward] = directions.forward,
  f = directions.forward,
  ahead = directions.forward,
  forward = directions.forward,
  forwards = directions.forward,

  [directions.backward] = directions.backward,
  b = directions.backward,
  back = directions.backward,
  backward = directions.backward,
  backwards = directions.backward,

  [directions.up] = directions.up,
  u = directions.up,
  up = directions.up,
  upward = directions.up,
  upwards = directions.up,

  [directions.down] = directions.down,
  d = directions.down,
  down = directions.down,
  downward = directions.down,
  downwards = directions.down,

  [directions.left] = directions.left,
  l = directions.left,
  left = directions.left,

  [directions.right] = directions.right,
  r = directions.right,
  right = directions.right
}

local function parseDirection(value)
  return assert(directionAliases[value], "invalid direction")
end

local function waitForLastAction()
  local id = robot:getLastActionId()

  local result = robot:getActionResult(id)
  while result and result == "INCOMPLETE" do
    sleep(100)
    result = robot:getActionResult(id)
  end

  if result == "SUCCESS" then
    return true
  else
    return false
  end
end

M.move = function(direction)
  M.moveAsync(direction)
  return waitForLastAction()
end

M.moveAsync = function(direction)
  direction = parseDirection(direction or directions.forward)
  while not robot:move(direction) do
    sleep(100)
  end
end

M.turn = function(direction)
  M.turnAsync(direction)
  return waitForLastAction()
end

M.turnAsync = function(direction)
  direction = parseDirection(direction)
  while not robot:turn(direction) do
    sleep(100)
  end
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

M.drop = function(count, direction)
  robot:drop(count or 1, direction or directions.forward)
end

return M