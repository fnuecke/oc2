local harness = require("harness")

local process = io.popen("tcc -I/mnt/builtin/include -run /mnt/builtin/example/devices.c 2>&1")
local output = process:read("a")
process:close()
print(output)

local system, redstone
for line in output:gmatch("[^\n]+") do
  local name = line:match("^%d+\t(%S+)")
  if name == "SYSTEM" then
    system = line
  elseif name == "REDSTN" then
    redstone = line
  end
end

harness.expect("system device is index 0", system and system:match("^(%d+)\t"), "0")
harness.expect("redstone interface is listed", redstone ~= nil, true)
harness.expect("label is among its names", redstone and redstone:find("test_device", 1, true) ~= nil, true)
harness.expect("block id is among its names", redstone and redstone:find("oc2:redstone_interface", 1, true) ~= nil, true)

harness.report()
