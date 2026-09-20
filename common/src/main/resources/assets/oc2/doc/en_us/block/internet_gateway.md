# Internet Gateway
![Wired](block:oc2:internet_gateway)

The internet gateway connects a network of [network connectors](network_connector.md) to the world outside the game, so every [computer](computer.md) on that network can use it. Attach it to a [network connector](network_connector.md) the same way a computer is attached.

**Servers decide whether this works at all.** Operators choose which addresses are allowed, and can switch it off entirely.

The gateway consumes energy for every packet it transmits, in both directions, and drops packets when insufficiently powered.

## Setting Up a Computer

Computers access the gateway through an ordinary [network interface card](../item/network_interface_card.md), so under the default Linux distribution this is just ethernet and the usual tools work.

There is no DHCP server behind the gateway, so pick the addresses yourself. Any private range will do, as long as the gateway address you name is on the same subnet as the computer:

```
ip addr add 10.0.0.2/24 dev eth0
ip link set eth0 up
ip route add default via 10.0.0.1
```

The gateway answers to whichever address you point the default route at. There is no name server behind it either, so set one in `/etc/resolv.conf`. Name lookups then travel as ordinary traffic, and are subject to the same address restrictions as everything else.

The commands above last until the next reboot. `setup-network.lua` does all of this and writes it to `/etc/network/interfaces`, so it survives a reboot. It will also offer to run a DHCP server on the computer. Every other computer on the network then needs no setup at all: it asks the DHCP server for an address, the route out and a name server.

TCP, UDP and ping are supported. Connections can only be opened outwards: nothing on the internet can connect to a computer.

`https://` addresses work, but **without certificate verification**, because the default Linux does not ship a list of trusted authorities. The traffic is protected from passive observers, but there is no guarantee the remote is not a man-in-the-middle. Treat it accordingly, and do not send passwords you care about through it.

Connections do not survive the chunk unloading or the server restarting. Treat this as a temporary connection failure and handle it accordingly.
