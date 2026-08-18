local harness = require("harness")
local expect, report = harness.expect, harness.report

local suites = os.getenv("OC2TEST_SUITES")
expect("suite directory is known", suites ~= nil, true)

local child = suites .. "/rpc_roundtrip/concurrent_child.lua"
local iterations = 20

-- A side each, so no two children ever write the same value.
local plan = {
  { side = "up", value = 15 },
  { side = "down", value = 7 },
  { side = "north", value = 3 },
}

local handles = {}
for i, entry in ipairs(plan) do
  handles[i] = io.popen(string.format("lua %s %s %d %d 2>&1",
                                      child, entry.side, entry.value, iterations), "r")
end
expect("every child started", #handles, #plan)

-- They are already running; collecting them one after another does not serialise them.
local output = {}
for i, handle in ipairs(handles) do
  output[i] = handle:read("a") or ""
  handle:close()
end

local latestStart, earliestEnd
for i, entry in ipairs(plan) do
  local text = output[i]
  expect("child on " .. entry.side .. " finished cleanly",
         text:find("\nok", 1, true) ~= nil or text:find("^ok") ~= nil,
         true)
  if text:find("bad", 1, true) then
    print("     " .. text:gsub("\n", "\n     "))
  end

  local started = tonumber(text:match("start (%d+)"))
  local ended = tonumber(text:match("end (%d+)"))
  if started and ended then
    latestStart = (not latestStart or started > latestStart) and started or latestStart
    earliestEnd = (not earliestEnd or ended < earliestEnd) and ended or earliestEnd
  end
end

-- Without this the suite would still pass if the children had merely queued up and taken turns,
-- which is exactly the behaviour the daemon exists to replace.
expect("all children were running at the same time",
       latestStart ~= nil and earliestEnd ~= nil and latestStart < earliestEnd, true)

-- Each child left its own side set, and the daemon kept those writes apart.
local bus = require("devices")
local redstone = bus:find("redstone")
for _, entry in ipairs(plan) do
  expect("side " .. entry.side .. " still holds its own value",
         redstone:getRedstoneOutput(entry.side), entry.value)
end

for _, entry in ipairs(plan) do
  redstone:setRedstoneOutput(entry.side, 0)
end

report()
