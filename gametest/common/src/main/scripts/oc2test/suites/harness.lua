local harness = { failures = 0, checks = 0 }

function harness.show(value)
  if type(value) ~= "string" then
    return tostring(value)
  end
  return '"' .. value:gsub("[^%g ]", function(c)
    return string.format("\\x%02x", c:byte())
  end) .. '"'
end

local function pass(name, detail)
  harness.checks = harness.checks + 1
  print(string.format("ok   %-52s %s", name, detail))
end

local function fail(name, detail)
  harness.checks = harness.checks + 1
  harness.failures = harness.failures + 1
  print(string.format("FAIL %-52s %s", name, detail))
end

function harness.expect(name, got, want)
  if got == want then
    pass(name, harness.show(got))
  else
    fail(name, string.format("got %s want %s", harness.show(got), harness.show(want)))
  end
end

function harness.raises(name, needle, fn)
  local ok, err = pcall(fn)
  if not ok and tostring(err):find(needle, 1, true) then
    pass(name, "raised")
  else
    fail(name, string.format("ok=%s err=%s", tostring(ok), tostring(err)))
  end
end

function harness.report()
  print("\nchecks " .. harness.checks)
  print(harness.failures == 0 and "ALL PASS"
        or (harness.failures .. " FAILURE(S)"))
  os.exit(harness.failures == 0 and 0 or 1)
end

return harness
