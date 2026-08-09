# Network Tunnel Module
![Tunnel vision?](item:oc2:network_tunnel_module)

The network tunnel module allows [robots](robot.md) to send messages to and receive messages from another tunnel device (tunnel modules and [tunnel cards](network_tunnel_card.md)) linked to the module.

To link two tunnel devices, open their configuration interface (use while holding), and insert the other tunnel device to link to. This allows linking any two network tunnel devices.

When using the default Linux distribution, this device will provide a regular ethernet device. Network setup can either be performed manually, or using the convenience script `setup-network.lua`. This script provides the option to either use a fixed-address setup, or a DHCP setup. For a DHCP setup, exactly one computer in the network must act as a DHCP server.

After initial setup, use the command `ifconfig` to see the currently used IP address.

Robots *have to be shut down* before installing or removing this component. Installing it while the robot is running will have no effect, removing it may lead to system errors.
