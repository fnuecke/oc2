#!/usr/bin/lua

local devices = require('devices')

local rs = devices:find("redstone")
if not rs then
	io.stderr:write("This program requires a Redstone Interface Card or Redstone Interface.\n")
	return 1
end

local args = table.pack(...)
if #args == 0 then
	io.write("Usage:\n")
	io.write("  redstone <side name> [<value>]\n")
	return
end

if #args > 0 then
	local side = args[1]
	sides = {["up"]=true,["down"]=true,["north"]=true,["south"]=true,["west"]=true,["east"]=true}
	if not sides[string.lower(side)] then
		io.stderr:write("invalid side\n")
		return
	end
	
	local value = args[2]
	if value then 
		if tonumber(value) then
			value = tonumber(value)
		else
			value = ({["true"]=true,["on"]=true,["yes"]=true})[value] and 15 or 0
		end
		rs:setRedstoneOutput(side, value)
	end
	io.write("in: " .. math.ceil(rs:getRedstoneInput(side)) .. "\n")
	io.write("out: " .. math.ceil(rs:getRedstoneOutput(side)) .. "\n")
end
