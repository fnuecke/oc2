# Redstone Interface
![Everything Red](block:oc2:redstone_interface)

The redstone interface provides an omnidirectional bus to receive and emit redstone signals.

Use this to interact with primitive devices, such as doors and lamps, or other machinery offering a redstone based protocol.

It can be controlled from Lua using the [high-level API](../hlapi.md), and from a [Z80](../item/cpu_z80.md) using the [mid-level API](../mlapi.md). The default Linux distribution offers Lua libraries for the former. For example:  
`local d = require("devices")`  
`local r = d:find("redstone")`  
`r:setRedstoneOutput("up", 15)`

## API
Programs control the redstone interface through the `redstone` device. See the [redstone device](../device/redstone.md) reference for its methods and how sides are named.
