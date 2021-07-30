# Robot
![I, for one, welcome our new robot overlords](item:oc2:robot)

Robots are essentially mobile [computers](../block/computer.md). Due to their non-stationary nature, there is some behavior that differs from regular computers. They cannot connect to [bus interfaces](../block/bus_interface.md). Instead of card devices, they support module devices. These are specialized devices taking into account the robots' mobility.

Robots have a fixed-size inventory and sport a state-of-the-art energy storage. Only the regular inventory of robots can be automatically filled and emptied, for example by devices such as a hopper. The component inventory of the robot must be manually configured.

In their default configuration, robots cannot interact with their own inventory. Use an [inventory operations module](inventory_operations_module.md) to enable robots to move items in their own inventory, as well as to insert and extract items to and from other inventories.

To recharge a robot, it is recommended to make use the [charger](../block/charger.md). It is possible for robots to recharge themselves by simply moving on top of a charger. Alternatively, they may be placed into an inventory on top of the charger.

The default Linux distribution provides a utility Lua library, `robot`, that eases controlling robots. The underlying API offers asynchronous methods for movement. The library implements synchronous alternatives, making sequential programming more convenient.

## API
Device name: `robot`

This is a high level API device. It can be controlled using Lua in the default Linux distribution. For example:  
`local d = require("devices")`  
`local r = d:find("robot")`  
`d:move("forward")`

### Directions
The direction parameter in the following methods represents a direction relative to the robot. Valid values are: `forward`, `backward`, `upward`, `downward` for movement actions, `left` and `right` for rotation actions. These directions are always from the point of view of the robot at the time it executes an action.

Short form aliases of these values can be used for convenience: `back`, `up`, `down`. For extreme brevity, the initial letter of each direction can be used as well.

### Methods
These methods are available on the underlying robot device. Note that the library offers useful wrapper for all of these. It is recommended to use the library instead of interacting with the device directly.

`getEnergyStored():number` returns the current amount of energy stored in the robot's internal energy storage.
- Returns the stored amount of energy.

`getEnergyCapacity():number` returns the maximum amount of energy that can be stored in the robot's internal energy storage.
- Returns the maximum amount of energy stored.

`getSelectedSlot():number` returns the currently selected robot inventory slot. This is used by many modules as an implicit input.
- Returns the index of the selected inventory slot.

`setSelectedSlot(slot:number):number` sets the currently selected robot inventory slot. This is used by many modules as an implicit input.
- `slot` is the index of the inventory slot to select.
- Returns the index of the newly selected slot. This may differ from `slot` if the specified value was invalid.

`getStackInSlot(slot:number):table` gets a description of the item in the specified slot.
- `slot` is the index of the slot to get the item description for.

`move(direction):boolean` tries to enqueue a movement action in the specified direction.
- `direction` is the direction to move in.
- Returns whether the action was enqueued successfully.

`turn(direction):boolean` tries to enqueue a turn action towards the specified direction.
- `direction` is the direction to turn towards.
- Returns whether the action was enqueued successfully.

`getLastActionId():number` returns the opaque id of the last enqueued action. Call this after a successful `move()` or `turn()` call to obtain the id associated with the enqueued action.
- Returns the id of the last enqueued action.

`getQueuedActionCount():number` returns the number of actions currently waiting in the action queue to be processed. Use this to wait for actions to finish when enqueueing fails.
- Returns the number of currently pending actions.

`getActionResult(actionId:number):string` returns the result of the action with the specified id. Action ids can be obtained from `getLastActionId()`. Only a limited number of past action results are available.
- Returns the result for the specified action id, or nothing if unavailable. When available, possible values are: `INCOMPLETE`, `SUCCESS` and `FAILURE`.

### Library API
- Library name: `robot`

This is a Lua library. It can be used in the default Linux distribution. For example:  
`local r = require("robot")`  
`r.move("forward")`  
`r.turn("left")`

### Methods
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

`move(direction):boolean` tries to move into the specified direction. Blocks until the movement operation has completed.
- `direction` is the direction to move in.
- Returns whether the operation was successful.

`moveAsync(direction)` tries to asynchronously move into the specified direction. Blocks until the action was successfully enqueued.
- `direction` is the direction to move in.

`turn(direction):boolean` tries to turn towards the specified direction. Blocks until the rotation operation has completed.
- `direction` is the direction to turn towards.
- Returns whether the operation was successfully.

`turnAsync(direction)` tries to asynchronously turn into the specified direction. Blocks until the action was successfully enqueued.
- `direction` is the direction to turn towards.