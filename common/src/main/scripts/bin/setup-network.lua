#!/usr/bin/lua

io.write("Setup DHCP client? [y/N]: ")
io.flush()
local dhcpClient = io.read()
if dhcpClient == "y" then
    local hostname
    local hostnameFile = io.open("/etc/hostname", "r")
    if hostnameFile then
        hostname = hostnameFile:read("l")
        hostnameFile:close()
    end

    local file = assert(io.open("/etc/network/interfaces", "a"))
    file:write("\n")
    file:write("auto eth0\n")
    file:write("iface eth0 inet dhcp\n")
    if hostname and hostname ~= "" then
        file:write("  hostname " .. hostname .. "\n")
    end

    assert(file:close())

    os.execute("ifup eth0")

    os.exit(0)
end

local ip, ipA, ipB, ipC, ipD
while true do
    io.write("IP address [192.168.0.1]: ")
    io.flush()
    ip = io.read()
    if not ip or ip == "" then
        ip = "192.168.0.1"
    end

    ipA, ipB, ipC, ipD = ip:match("(%d+)%.(%d+)%.(%d+)%.(%d+)")
    if not ipA then
        io.write("Invalid IP address format.")
    else
        break
    end
end

local mask
while true do
    io.write("Netmask [255.255.255.0]: ")
    io.flush()
    mask = io.read()
    if not mask or mask == "" then
        mask = "255.255.255.0"
    end

    if not mask:match("%d+%.%d+%.%d+%.%d+") then
        io.write("Invalid IP mask format.")
    else
        break
    end
end

local defaultGateway = ipA .. "." .. ipB .. "." .. ipC .. ".254"
local gateway
while true do
    io.write("Internet gateway address, '-' for none [" .. defaultGateway .. "]: ")
    io.flush()
    local input = io.read()
    if not input or input == "" then
        gateway = defaultGateway
        break
    elseif input == "-" then
        gateway = ""
        break
    elseif input:match("^%d+%.%d+%.%d+%.%d+$") then
        gateway = input
        break
    else
        io.write("Invalid IP address format. Enter '-' for none.\n")
    end
end

local nameServer
if gateway ~= "" then
    while true do
        io.write("Name server [1.1.1.1]: ")
        io.flush()
        nameServer = io.read()
        if not nameServer or nameServer == "" then
            nameServer = "1.1.1.1"
        end

        if not nameServer:match("^%d+%.%d+%.%d+%.%d+$") then
            io.write("Invalid IP address format.\n")
        else
            break
        end
    end
end

local file = assert(io.open("/etc/network/interfaces", "a"))
file:write("\n")
file:write("auto eth0\n")
file:write("iface eth0 inet static\n")
file:write("  address " .. ip .. "\n")
file:write("  netmask " .. mask .. "\n")
if gateway ~= "" then
    file:write("  gateway " .. gateway .. "\n")
end

assert(file:close())

if nameServer then
    local resolv = assert(io.open("/etc/resolv.conf", "w"))
    resolv:write("nameserver " .. nameServer .. "\n")
    assert(resolv:close())
end

os.execute("ifup eth0")

io.write("Setup DHCP server? [y/N]: ")
io.flush()
local dhcpServer = io.read()
if dhcpServer == "y" or dhcpServer == "Y" then
    local dhcpRangeStart
    while true do
        io.write("DHCP range start [" .. ipA .. "." .. ipB .. "." .. ipC .. ".200]: ")
        io.flush()
        dhcpRangeStart = io.read()
        if not dhcpRangeStart or dhcpRangeStart == "" then
            dhcpRangeStart = ipA .. "." .. ipB .. "." .. ipC .. ".200"
        end

        if not dhcpRangeStart:match("%d+%.%d+%.%d+%.%d+") then
            io.stderr:write("Invalid IP address format.")
        else
            break
        end
    end

    local dhcpRangeEnd
    while true do
        io.write("DHCP range end [" .. ipA .. "." .. ipB .. "." .. ipC .. ".250]: ")
        io.flush()
        dhcpRangeEnd = io.read()
        if not dhcpRangeEnd or dhcpRangeEnd == "" then
            dhcpRangeEnd = ipA .. "." .. ipB .. "." .. ipC .. ".250"
        end

        if not dhcpRangeEnd:match("%d+%.%d+%.%d+%.%d+") then
            io.stderr:write("Invalid IP address format.")
        else
            break
        end
    end

    local file = assert(io.open("/etc/dnsmasq.conf", "a"))
    file:write("dhcp-range="..dhcpRangeStart..","..dhcpRangeEnd..","..mask..",12h\n")
    file:write("dhcp-authoritative\n")
    if gateway ~= "" then
        file:write("dhcp-option=3,"..gateway.."\n")
        file:write("dhcp-option=6,"..ip.."\n")
    end

    assert(file:close())

    os.execute("if [ -f /etc/init.d/dnsmasq ]; then mv /etc/init.d/dnsmasq /etc/init.d/S80dnsmasq; fi")
    os.execute("/etc/init.d/S80dnsmasq start")
end
