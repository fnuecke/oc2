# Internet Gateway
![Wired](block:oc2:internet_gateway)

The internet gateway connects a network of [network connectors](network_connector.md) to the world outside the game, so every [computer](computer.md) on that network can use it. Attach it to a [network connector](network_connector.md) the same way a computer is attached.

**Servers decide whether this works at all.** Operators choose which addresses are allowed, and can switch it off entirely.

The gateway consumes energy for every packet it transmits, in both directions, and drops packets when insufficiently powered.

## Setting Up a Computer
Computers access the gateway through an ordinary [network interface card](../item/network_interface_card.md). Note that only [Linux](../systems/linux.md) supports networking.

The gateway has a configurable address, `192.168.0.254` by default. Open the settings screen by using it.

The gateway does not provide a DHCP service, so computers have to be assigned addresses explicitly, or by running a DHCP server on one of the computers. To use the gateway with a manually set address:

`ip addr add 192.168.0.2/24 dev eth0`
`ip link set eth0 up`
`ip route add default via 192.168.0.254`

It also does not provide name servers, so set one in `/etc/resolv.conf`. Name lookups then travel as ordinary traffic, and are subject to the same address restrictions as everything else.

The commands above last until the next reboot. `setup-network.lua` does all of this and writes it to `/etc/network/interfaces`, so it survives a reboot. It will also offer to run a DHCP server on the computer. Every other computer on the network then needs no setup at all: it asks the DHCP server for an address, the route out and a name server.

TCP, UDP and ping are supported. Connections can only be opened outwards: nothing on the internet can connect to a computer.

`https://` addresses work, but **without certificate verification**, because Linux does not ship a list of trusted authorities. The traffic is protected from passive observers, but there is no guarantee the remote is not a man-in-the-middle. Treat it accordingly, and do not send passwords you care about through it.

Connections do not survive the chunk unloading or the server restarting. Treat this as a temporary connection failure and handle it accordingly.

## VXLAN
A second mode can be enabled in the mods' server configuration, Virtual Extensible LAN (VXLAN). In this mode the gateway connects the computers to a network that the server operator set up outside the game. Computers then appear on that network directly, without network address translation (NAT) or the restrictions above. Whether that network provides addresses, routes and name servers depends on its setup.

When available, switch the mode in the gateway's settings screen, then enter the remote peer, port and VXLAN network identifier (VNI). Ask your server operator for these values.
