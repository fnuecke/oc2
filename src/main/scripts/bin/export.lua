#!/usr/bin/lua

local devices = require("devices")
local device = devices:find("file_import_export")

if not device then
    io.write("A File Import/Export Card is required for this functionality.\n")
    return
end

if not arg[1] then
    io.write("Usage: export.lua filename\n")
    os.exit(1)
end

local file = assert(io.open(arg[1], "rb"))

device:reset()

io.write("Exporting ")
io.flush()

device:beginExportFile(arg[1])

while true do
    local str = file:read(512)
    if not str then break end
    if #str > 0 then
        local bytes = {string.byte(str, 1, -1)}
        device:writeExportFile(bytes)
        io.write(".")
        io.flush()
    end
end
io.write("\n")

device:finishExportFile()

assert(file:close())
