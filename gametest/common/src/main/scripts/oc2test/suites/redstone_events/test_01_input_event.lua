local bus = require("devices")
local redstone = assert(bus:find("redstone"), "no redstone interface on the bus")

assert(redstone:getRedstoneInput("up") == 0, "the top side is already powered")
redstone:setRedstoneOutput("south", 15) -- tells Java side guestReceivesInputChanges to continue

local event = redstone:waitEvent(10000, "redstoneChanged")
assert(event, "no redstoneChanged event arrived within 10s")
assert(event.data.side == "up", "the event named side " .. tostring(event.data.side))
assert(event.data.value == 15, "the event reported level " .. tostring(event.data.value))
assert(redstone:getRedstoneInput("up") == 15, "the input does not match the event")
redstone:setRedstoneOutput("south", 14) -- tells guestReceivesInputChanges we're good
event = redstone:waitEvent(10000, "redstoneChanged")
assert(event, "no event for the signal going away")
assert(event.data.side == "up" and event.data.value == 0,
  string.format("the second event was %s=%s", tostring(event.data.side), tostring(event.data.value)))

require("harness").report()
