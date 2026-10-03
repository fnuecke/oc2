local bus = require("devices")
local harness = require("harness")
local expect = harness.expect

local scanner = assert(bus:find("scanner"), "no scanner device on the bus")

local scan = scanner:scan()
expect("hardness arrives as binary", type(scan.hardness), "string")
expect("hardness covers the cube", #scan.hardness, 343)
expect("the robot's own space is air", string.byte(scan.hardness, 3 * 49 + 3 * 7 + 3 + 1), 0)
expect("entities arrive as a list", type(scan.entities), "table")
expect("a second scan fails while recharging", (pcall(scanner.scan, scanner)), false)

local inspection = scanner:inspect("front")
expect("inspect returns a table", type(inspection), "table")
expect("its entities are a list", type(inspection.entities), "table")
expect("the sky is a yes or no", type(scanner:canSeeSky()), "boolean")

harness.report()
