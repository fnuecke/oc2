# Network Interface Card
![Talk to strangers](item:oc2:network_interface_card)

The network interface card (NIC) allows [computers](../block/computer.md) to send messages to and receive messages from [network connectors](../block/network_connector.md) attached to the computer.

These cards can be configured to only connect to selected sides (use while holding). This allows using multiple cards to build a custom router, for example.

Only [Linux](../systems/linux.md) supports this device. It provides a regular ethernet device. Network setup can either be performed manually, or using the convenience script `setup-network.lua`. This script provides the option to either use a fixed-address setup, or a DHCP setup. For a DHCP setup, exactly one computer in the network must act as a DHCP server. See [networking](../networking.md) for details.

After initial setup, use the command `ifconfig` to see the currently used IP address.

Computers *have to be shut down* before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.
