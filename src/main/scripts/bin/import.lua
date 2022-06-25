#!/usr/bin/lua

local devices = require("devices")
local device = devices:find("file_import_export")

if not device then
    io.stderr:write("A File Import/Export Card is required for this functionality.\n")
    os.exit(1)
end

device:reset()

if not device:requestImportFile() then
    io.write("No users present to request file from.\n")
    os.exit(1)
end

local name, size
while true do
    local info = device:beginImportFile()
    if info then
        name = arg[1] or info.name or "imported"
        size = info.size
        break
    end
end

local function file_exists(path)
    local f = io.open(path, "r")
    if f then
        f:close()
        return true
    else
        return false
    end
end

while file_exists(name) do
    io.write("File '" .. name .. "' exists. [O]verwrite/[U]se another name/[C]ancel? ")
    io.flush()
    local choice = io.read()
    if not choice or choice == "" or choice == "c" or choice == "C" then
        os.exit(0)
    elseif choice == "u" or choice == "U" then
        io.write("Enter new name: ")
        io.flush()
        name = io.read()
    else
        break
    end
end

io.write("Importing ")
io.flush()

local file = assert(io.open(name, "wb"))

local readCount = 0
local lastPrintedPercent = 0
while true do
    local bytes = device:readImportFile()
    if not bytes then break end
    if #bytes > 0 then
        file:write(string.char(table.unpack(bytes)))

        readCount = readCount + #bytes
        local percent = math.floor(100 * readCount / size)
        if percent >= lastPrintedPercent + 5 then
            io.write("\n" .. percent .. "% ")
            lastPrintedPercent = percent
        else
            io.write(".")
        end
        io.flush()
    end
end
io.write("\n")

assert(file:close())
