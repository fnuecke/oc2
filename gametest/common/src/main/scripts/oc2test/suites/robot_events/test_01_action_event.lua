local bus = require("devices")
local robot = assert(bus:find("robot"), "no robot device on the bus")

assert(robot:move("upward"), "the move was not queued")
local actionId = robot:getLastActionId()

local event = bus:waitEvent(10000, "robotActionCompleted")
assert(event, "no robotActionCompleted event arrived within 10s")
assert(event.type == "robotActionCompleted", "unexpected event type " .. tostring(event.type))

local data = assert(event.data, "event carried no data")
assert(data.actionId == actionId,
  string.format("event was for action %s, expected %s", tostring(data.actionId), tostring(actionId)))
assert(data.result == "SUCCESS", "action reported " .. tostring(data.result))

require("harness").report()
