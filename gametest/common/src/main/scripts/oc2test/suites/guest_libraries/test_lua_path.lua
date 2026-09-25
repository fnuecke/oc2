local harness = require("harness")
local expect, report = harness.expect, harness.report

expect("system Lua modules still resolve", (pcall(require, "cjson.util")), true)

report()
