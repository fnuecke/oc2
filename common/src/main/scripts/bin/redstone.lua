#!/usr/bin/lua

local devices = require('devices')
local rs = devices:find("redstone")

if not rs then
	io.stderr:write("This program requires a Redstone Interface Block or Card.\n")
    os.exit(1)
end

if #arg == 0 then
	io.write("Usage: redstone.lua side [value]\n")
    os.exit(1)
end

local function set_of(...)
    local set = {}
    for _, value in pairs(table.pack(...)) do
        set[value] = true
    end
    return set
end

local side = string.lower(arg[1])
local sides = set_of("up", "down", "north", "south", "west", "east", "front", "back", "left", "right")
if not sides[side] then
    io.stderr:write("Invalid side argument.\n")
    os.exit(1)
end

local value = arg[2]
if value then
    local on = set_of("true", "on", "yes")
    value = tonumber(value) or on[value] and 15 or 0
    rs:setRedstoneOutput(side, value)
end

io.write("in: " .. math.ceil(rs:getRedstoneInput(side)) .. "\n")
io.write("out: " .. math.ceil(rs:getRedstoneOutput(side)) .. "\n")
