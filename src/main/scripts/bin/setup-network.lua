#!/usr/bin/lua

io.write("IP address [192.168.0.1]: ")
io.flush()
local ip = io.read()
if not ip then
    return
end

io.write("Netmask [255.255.255.0]: ")
io.flush()
local mask = io.read()
if not mask then
    return
end

