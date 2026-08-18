local harness = require("harness")
local expect, raises, report = harness.expect, harness.raises, harness.report

local bus = require("devices")

expect("the bus reached the host through the daemon", bus.transport, "socket")

-- Discovery
local redstone = bus:find("redstone")
if not redstone then
  print("FAIL no redstone card on the bus")
  print("\n1 FAILURE(S)")
  os.exit(1)
end
expect("redstone card found on the bus", redstone.deviceId ~= nil, true)

-- Metadata, built from the host's annotations rather than from our own constants
local names = {}
for _, method in ipairs(bus:methods(redstone.deviceId)) do
  names[method.name] = true
end
expect("host lists setRedstoneOutput", names.setRedstoneOutput or false, true)
expect("host lists getRedstoneOutput", names.getRedstoneOutput or false, true)

-- Both dispatch paths: the setter is synchronized onto the server thread, the getter
-- answers straight from the VM thread.
redstone:setRedstoneOutput("up", 15)
expect("value set on the server thread reads back", redstone:getRedstoneOutput("up"), 15)
redstone:setRedstoneOutput("up", 0)
expect("and can be cleared again", redstone:getRedstoneOutput("up"), 0)

-- Errors
raises("unknown method is refused", "unknown method",
       function() return redstone:invoke("noSuchMethodExists") end)
raises("unknown device is refused", "unknown device",
       function() return bus:invoke("00000000-0000-0000-0000-000000000000",
                                    "getRedstoneOutput", "up") end)

-- Overflow: more than the host's 4 KiB message limit has to come back as an error rather
-- than as silence.
raises("oversized message is refused", "message too large", function()
  return redstone:invoke("setRedstoneOutput", "up", string.rep("x", 8 * 1024))
end)
expect("bus still usable after an oversized message", redstone:getRedstoneOutput("up"), 0)

-- Recovery
expect("device list still resolves", bus:find("redstone") ~= nil, true)

report()
