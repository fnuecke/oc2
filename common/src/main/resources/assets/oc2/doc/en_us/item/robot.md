# Robot
![I, for one, welcome our new robot overlords](item:oc2:robot)

Robots are essentially mobile [computers](../block/computer.md). Due to their non-stationary nature, there is some behavior that differs from regular computers. They cannot connect to [bus interfaces](../block/bus_interface.md). Instead of card devices, they support module devices. These are specialized devices taking into account the robots' mobility. In place of a second [hard drive](hard_drive.md) bay they have a [floppy](floppy.md) slot, since they cannot use [disk drives](../block/disk_drive.md).

Robots have a fixed-size inventory and sport a state-of-the-art energy storage. Only the regular inventory of robots can be automatically filled and emptied, for example by devices such as a hopper. The component inventory of the robot must be manually configured.

In their default configuration, robots cannot interact with their own inventory. Use an [inventory operations module](inventory_operations_module.md) to enable robots to move items in their own inventory, as well as to insert and extract items to and from other inventories.

To recharge a robot, it is recommended to make use the [charger](../block/charger.md). It is possible for robots to recharge themselves by simply moving on top of a charger. Alternatively, they may be placed into an inventory on top of the charger.

The default Linux distribution provides a utility Lua library, `robot`, that eases controlling robots. The underlying API offers asynchronous methods for movement. The library implements synchronous alternatives, making sequential programming more convenient.

For an overview of how robots move, how they draw energy and how to script them, see the [robotics](../robotics.md) entry.

## API
Programs control the robot through the `robot` device. See the [robot device](../device/robot.md) reference for its methods.

## Library
- Library name: `robot`

This is a Lua library. It can be used in the default Linux distribution. For example:  
`local r = require("robot")`  
`r.move("forward")`  
`r.turn("left")`

`energy():number` returns the current amount of energy stored in the robot's internal energy storage.
- Returns the stored amount of energy.

`capacity():number` returns the maximum amount of energy that can be stored in the robot's internal energy storage.
- Returns the maximum amount of energy stored.

`slot():number` returns the currently selected robot inventory slot. This is used by many modules as an implicit input.
- Returns the index of the selected inventory slot.

`slot(slot:number):number` sets the currently selected robot inventory slot. This is used by many modules as an implicit input.
- `slot` is the index of the inventory slot to select.
- Returns the index of the newly selected slot. This may differ from `slot` if the specified value was invalid.

`stack([slot:number]):table` gets a description of the item in the specified slot.
- `slot` is the index of the slot to get the item description for. Optional, defaults to `slot()`.

`statusColor([color:number]):number` gets, and optionally sets, the color of the status light.
- `color` is the color to set, as a packed `0xRRGGBB` value. Optional.
- Returns the color in effect after the call.

`statusValue([value:number]):number` gets, and optionally sets, how far the status light is filled.
- `value` is the fill value to set, in the range of [0, 1]. Optional.
- Returns the fill value in effect after the call.

`detect(side):string` reports what occupies the space on the specified side of the robot.
- `side` is the side to look at. See the "Sides" section of the [robot device](../device/robot.md).
- Returns `solid`, `fluid` or `air`. Since only `solid` stops the robot, `detect(side) ~= "solid"` tells you a move that way will not be blocked.

`move(direction[,timeout:number]):boolean` tries to move into the specified direction. Blocks until the movement operation has completed.
- `direction` is the direction to move in.
- `timeout` is how long to wait for, in milliseconds. Optional, defaults to 30000.
- Returns whether the operation was successful. Returns `false` if the timeout ran out first.

`moveAsync(direction[,timeout:number]):boolean` tries to asynchronously move into the specified direction. Blocks until the action was successfully enqueued.
- `direction` is the direction to move in.
- `timeout` is how long to wait for a free slot in the action queue, in milliseconds. Optional, defaults to 30000.
- Returns whether the action was enqueued. Returns `false` if the timeout ran out first.

`turn(direction[,timeout:number]):boolean` tries to turn towards the specified direction. Blocks until the rotation operation has completed.
- `direction` is the direction to turn towards.
- `timeout` is how long to wait for, in milliseconds. Optional, defaults to 30000.
- Returns whether the operation was successful. Returns `false` if the timeout ran out first.

`turnAsync(direction[,timeout:number]):boolean` tries to asynchronously turn into the specified direction. Blocks until the action was successfully enqueued.
- `direction` is the direction to turn towards.
- `timeout` is how long to wait for a free slot in the action queue, in milliseconds. Optional, defaults to 30000.
- Returns whether the action was enqueued. Returns `false` if the timeout ran out first.
