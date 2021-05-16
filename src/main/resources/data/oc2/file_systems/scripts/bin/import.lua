#!/usr/bin/lua

local devices = require("devices")
local cloud = devices:find("cloud")

if not cloud then
    print("A cloud interface card is required for this functionality.")
    return
end

if not arg[1] then
    io.write("Usage: import.lua filename\n")
    os.exit(1)
end

local file = assert(io.open(arg[1], "wb"))

cloud:reset()

io.write("Importing")

cloud:beginImportFile()

while true do
    local bytes = cloud:readImportFile()
    if not bytes then break end
    if #bytes > 0 then
        file:write(string.char(table.unpack(bytes)))
        io.write(".")
    end
end
io.write("\n")

assert(file:close())
