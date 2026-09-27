local harness = require("harness")
local bus = require("devices")
local redstone = assert(bus:find("redstone"), "no redstone interface on the bus")

local OUTPUT = "/tmp/redstone.out"

local function read(path)
  local file = io.open(path)
  if not file then
    return nil
  end
  local text = file:read("a")
  file:close()
  return text
end

local function interruptCount()
  for line in read("/proc/interrupts"):gmatch("[^\n]+") do
    if line:find("oc2-mlapi", 1, true) then
      return tonumber(line:match("^%s*%d+:%s*(%d+)"))
    end
  end
end

harness.expect("interrupt is registered", interruptCount() ~= nil, true)
local before = interruptCount() or 0

os.remove(OUTPUT)
os.execute("tcc -I/mnt/builtin/include -run /mnt/builtin/example/redstone.c > " .. OUTPUT .. " 2>&1 &")
local function waitFor(predicate)
  for _ = 1, 40 do
    local output = read(OUTPUT)
    if output and predicate(output) then
      return output
    end
    os.execute("sleep 0.5")
  end
  return read(OUTPUT)
end

local waiting = "waiting for a redstone change...\n"
harness.expect("example waits", waitFor(function(output) return output ~= "" end), waiting)
redstone:setRedstoneOutput("south", 15) -- tells Java side guestExampleWaitsForInputChange to continue

local output = waitFor(function(output) return output ~= waiting end)
harness.expect("example reports the change", output, waiting .. "side 1: in = 15\n")
harness.expect("interrupt fired", (interruptCount() or 0) > before, true)

harness.report()
