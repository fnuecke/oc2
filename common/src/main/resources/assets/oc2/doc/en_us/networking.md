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

## Tools
Once you have a network setup running, with multiple computers in one network, you have all the options in the world. For example, you can copy files between computers using `scp`, log in to a remote computer using `ssh` and write custom network programs in Lua using the `socket` library. For samples on how to use the `socket` library, please the [samples in the official repository](https://github.com/diegonehab/luasocket/tree/master/samples).