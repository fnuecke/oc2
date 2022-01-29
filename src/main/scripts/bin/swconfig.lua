#!/usr/bin/lua

function usage()
    print("Usage:")
    print("  swconfig show_hosts")
    print("  swconfig show_ports")
    print("  swconfig set_port <port> untagged <vid>")
    print("  swconfig set_port <port> trunk_all (on|off)")
end

if not arg[1] then
    usage()
    return
end

local cjson = require("cjson").new()

local devbus = require('devices')
local switch = devbus:find("switch")

if not switch then
    print("No switch found")
    return
end

if arg[1] == "show_hosts" then
    host_table = switch:getHostTable()

    for _, v in ipairs(host_table) do
        print(v.mac .. " : " .. v.side .. ", Age: " .. v.age)
    end
elseif arg[1] == "show_ports" then
    local link_state = switch:getLinkState()
    for i, port in ipairs(switch:getPortConfig()) do
        print("Port #" .. (i - 1) .. " " .. (link_state[i] and "UP" or "DOWN"))
        print("  Untagged VLAN: " .. port.untagged)
        print("  Tagged: " .. table.concat(port.tagged, ", "))
        print("  Hairpin: " .. (port.hairpin and "on" or "off"))
        print("  Trunk All: " .. (port.trunk_all and "on" or "off"))
    end
elseif arg[1] == "set_port" then
    if #arg < 4 then
        usage()
        return
    end
    local config = switch:getPortConfig()
    local port = config[tonumber(arg[2]) + 1]
    if not port then
        print("Invalid Port Number")
        return
    end
    if arg[3] == "untagged" then
        port.untagged = tonumber(arg[4])
    end
    switch:setPortConfig(config)
else
    usage()
    return
end
