#!/usr/bin/lua

io.write("Setup DHCP client? [y/N]: ")
io.flush()
local dhcpClient = io.read()
if dhcpClient == "y" then
    local file = assert(io.open("/etc/network/interfaces", "a"))
    file:write("\n")
    file:write("auto eth0\n")
    file:write("iface eth0 inet dhcp\n")

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

local file = assert(io.open("/etc/network/interfaces", "a"))
file:write("\n")
file:write("auto eth0\n")
file:write("iface eth0 inet static\n")
file:write("  address " .. ip .. "\n")
file:write("  netmask " .. mask .. "\n")

assert(file:close())

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

    assert(file:close())

    os.execute("/etc/init.d/*dnsmasq start")
end
