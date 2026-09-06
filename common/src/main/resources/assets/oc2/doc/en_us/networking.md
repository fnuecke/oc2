# Networking
To set up a [computer](block/computer.md) network, use [network connectors](block/network_connector.md) and [network cables](item/network_cable.md) to connect the connectors. Finally, make sure a [network interface card](item/network_interface_card.md) is installed in each computer that should be part of the network.

Connectors have to be attached to any face but the front face of a computer. This allows the network card installed in it to send and receive packets through the connector.

Connectors connected with cables will forward packets to each other. For more complex network setups, a [network hub](block/network_hub.md) may be necessary. It allows connecting multiple connectors with each other.

## Hops
Network packets can only travel a certain maximum number of "hops". A hop can generally be understood as a single forwarding operation. For example, a connector forwarding a packet uses one hop. A network hub forwarding a packet to each connector the packet did not arrive from uses one hop. Once the number of remaining hops reaches zero, the packet is no longer forwarded. This avoids packets going in circles forever for networks with cycles. That said, this is a safeguard mechanism. No network should contain cycles, as this will also lead to the same packet arriving multiple times on receiving computers.

## Computer Setup
To set up a computer for networking, first ensure a network card is present. After this, using the default Linux distribution, run the command `setup-network.lua`. This will provide a wizard to configure how the computer should connect to the network. Alternatively, if you know what you're doing, set up network as you would on any regular Linux installation.

## DHCP
DHCP is a protocol which allows a simplified network setup. Only one computer will need to have a statically configured network address, all other computers in the network may have their addresses assigned to them automatically. When going for this setup, ensure there is only a single computer that acts as a DHCP server. Also ensure that no computer uses a static IP address that falls into the range of dynamically distributed IP addresses.

`setup-network.lua` starts the DHCP server for you once you configure a computer as one. It does not survive a reboot, however. See the "Daemons" section below for how to start it again, or how to make it start on its own.

## Tools
Once you have a network setup running, with multiple computers in one network, you have all the options in the world. For example, you can copy files between computers using `scp`, log in to a remote computer using `ssh` and write custom network programs in Lua using the `socket` library. For samples on how to use the `socket` library, please see the [samples in the official repository](https://github.com/diegonehab/luasocket/tree/master/samples).

Note that `scp` and `ssh` are the client tools. Reaching a computer *from* elsewhere needs a server running on it. See the "Daemons" section below.

## The Internet
To reach a world outside this dimension, a network needs an [internet gateway](block/internet_gateway.md) on it. Computers then use their ordinary [network interface cards](item/network_interface_card.md) and route through it.

There is no DHCP and no name server on the far side, so set an address, a route and a name server yourself. See the [internet gateway](block/internet_gateway.md) entry for an example.

## Daemons
To keep idle computers cheap, the network daemons are shipped but not started at boot. Start the one you need by hand:

`/etc/init.d/dropbear start` for the `ssh` and `scp` server  
`/etc/init.d/telnet start` for the `telnet` server  
`/etc/init.d/dnsmasq start` for the DHCP server

The `ssh` server refuses to let anyone in while an account has a blank password, which is what `root` ships with. Set one with `passwd` before connecting for the first time. The `telnet` server has no such restriction, and accepts the blank password as-is, which also means anyone who reaches the computer can log into it.

None of these survive a reboot. To have one start on its own, give its script a boot-sequence prefix, which is what the boot scripts look for:  
`mv /etc/init.d/dropbear /etc/init.d/S50dropbear`
