#!/usr/bin/lua

local devices = require("devices")
local device = devices:find("file_import_export")

if not device then
    print("A File Import/Export Card is required for this functionality.")
    return
end

if not arg[1] then
    io.write("Usage: import.lua filename\n")
    os.exit(1)
end

local file = assert(io.open(arg[1], "wb"))

device:reset()

io.write("Importing")

device:beginImportFile()

while true do
    local bytes = device:readImportFile()
    if not bytes then break end
    if #bytes > 0 then
        file:write(string.char(table.unpack(bytes)))
        io.write(".")
        io.flush()
    end
end
io.write("\n")

assert(file:close())
