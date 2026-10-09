# Bus Interface
![Plug and play](item:oc2:bus_interface)

Bus interfaces connect external devices to [computers](computer.md). This includes explicit device blocks, such as the [redstone interface](redstone_interface.md). Some generic functionality blocks of blocks is also available, such as information on energy storage.

It is possible to configure an explicit name for a bus interface using a [wrench](../item/wrench.md). This is useful when attaching multiple devices of the same type to a computer: when searching devices by type name (`devices:find(typeName)` on the HLAPI, `OCFIND` on the MLAPI), these custom names will also work. On the HLAPI, multiple devices on a single face are merged. On the MLAPI they are not, because method indices could collide. Use both label and type name to find the desired device. Names are limited to printable ASCII characters.
 
Note that [computers](computer.md) must also be explicitly connected to a [bus](bus_cable.md) with a bus connector.
