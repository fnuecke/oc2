#!/usr/bin/lua

local devices = require("devices")
local cloud = devices:find("cloud")

if not cloud then
    print("A cloud interface card is required for this functionality.")
    return
end

if not arg[1] then
    io.write("Usage: export.lua filename\n")
    os.exit(1)
end

local file = assert(io.open(arg[1], "rb"))

cloud:reset()

io.write("Exporting")

cloud:beginExportFile(arg[1])

while true do
    local str = file:read(512)
    if not str then break end
    if #str > 0 then
        local bytes = {string.byte(str, 1, -1)}
        cloud:writeExportFile(bytes)
        io.write(".")
    end
end
io.write("\n")

cloud:finishExportFile()

assert(file:close())
