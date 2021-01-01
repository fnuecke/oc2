#!/usr/bin/lua

local devices = require("devices")
local json = require("cjson").new()
for _,device in ipairs(devices:list()) do
    local line = device.deviceId .. "\t"
    local isFirstTypeName = true
    table.sort(device.typeNames)
    for _,typeName in ipairs(device.typeNames) do
        if isFirstTypeName then
            isFirstTypeName = false
        else
            line = line .. ", "
        end
        line = line .. typeName
    end
    print(line)
end
