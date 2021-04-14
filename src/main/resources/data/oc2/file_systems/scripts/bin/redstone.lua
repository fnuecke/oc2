#!/usr/bin/lua
--Redstone utility port from OpenComputers
--Demonstration of programming in Lua in OpenComputers II
--Written by Bs()Dd in 2021

local devices = require('devices')

if not devices:find("redstone") then
  io.stderr:write("This program requires a Redstone Interface Card or Redstone Interface.\n")
  return 1
end
local rs = devices:find("redstone")

local args = table.pack(...)
if #args == 0 then
  io.write("Usage:\n")
  io.write("  redstone <side> [<value>]\n")
  return
end

if #args > 0 then
  local side = args[1]
  if tonumber(side) then
	side = tonumber(side)
  else
    sides = {["up"]=true,["down"]=true,["north"]=true,["south"]=true,["west"]=true,["east"]=true}
	if not sides[string.lower(side)] then
	  io.stderr:write("invalid side\n")
	  return
	end
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
  io.write("in: " .. rs:getRedstoneInput(side) .. "\n")
  io.write("out: " .. rs:getRedstoneOutput(side) .. "\n")
end