local side, value, iterations = ...
value = tonumber(value)
iterations = tonumber(iterations)

local clock = require("oc2.clock")
local bus = require("devices")

if bus.transport ~= "socket" then
  print("bad transport " .. tostring(bus.transport))
  os.exit(1)
end

local redstone = bus:find("redstone")
if not redstone then
  print("bad no redstone device")
  os.exit(1)
end

print("start " .. clock.ms())

redstone:setRedstoneOutput(side, value)
for i = 1, iterations do
  local got = redstone:getRedstoneOutput(side)
  if got ~= value then
    print(string.format("bad %s iteration %d got %s want %d", side, i, tostring(got), value))
    os.exit(1)
  end
end

print("end " .. clock.ms())
print("ok")
