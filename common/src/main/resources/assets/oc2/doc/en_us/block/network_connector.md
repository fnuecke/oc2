# Network Connector
![The data must flow](block:oc2:network_connector)

The network connector is a fundamental part for building a network connecting different [computers](computer.md). Use [network cables](../item/network_cable.md) to connect connectors with each other.

Each connector will represent one "hop" for relayed packets. Packets may only travel a limited number of hops before they are dropped.

Note that each connector only supports up to two attached network cables. Chain multiple connectors to increase the range that can be spanned.

Create a simple multi-computer network by chaining the connectors attached to the computers. To create a mesh network, use a [network hub](network_hub.md). The hub connects all attached connectors with each other.