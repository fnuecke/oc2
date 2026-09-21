# Robot

## High-level API
Device name: `robot`

Provided by the [robot](../item/robot.md).

### Directions
The direction parameter in the following methods represents a direction relative to the robot. Valid values are: `forward`, `backward`, `upward`, `downward` for movement actions, `left` and `right` for rotation actions. These directions are always from the point of view of the robot at the time it executes an action.

Short form aliases of these values can be used for convenience: `back`, `up`, `down`. For extreme brevity, the initial letter of each direction can be used as well.

### Sides
The side parameter of `detect()` represents a face of the robot rather than a movement direction. Valid values are: `front`, `up` and `down`.

Note that the `robot` Lua library described in the [robot](../item/robot.md) entry offers useful wrappers for all of these methods. It is recommended to use the library instead of interacting with the device directly.

### Methods

`detect(side:string):string`
Reports what occupies the space on the specified side of the robot. This only tells you whether the space is free, not what is in it.
- `side`: the side to look at: `front`, `up` or `down`.
- Returns `solid` if something there blocks movement, `fluid` if the space holds a fluid the robot can move through, or `air` if the space is free. Blocks the robot can pass through, such as grass, count as `air`.

`getActionResult(actionId:number):string`
Gets the result of the action with the specified id. Action ids can be obtained from `getLastActionId()`. Only a limited number of past action results are available.
- `actionId`: the id of the action to get the result for.
- Returns the result for the specified action id, or nothing if unavailable. When available, possible values are: `INCOMPLETE`, `SUCCESS` and `FAILURE`.

`getEnergyCapacity():number`
Gets the maximum amount of energy that can be stored in the robot's internal energy storage.
- Returns the maximum amount of energy stored.

`getEnergyStored():number`
Gets the current amount of energy stored in the robot's internal energy storage.
- Returns the stored amount of energy.

`getLastActionId():number`
Gets the opaque id of the last enqueued action. Call this after a successful `move()` or `turn()` call to obtain the id associated with the enqueued action.
- Returns the id of the last enqueued action.

`getQueuedActionCount():number`
Gets the number of actions currently waiting in the action queue to be processed. Use this to wait for actions to finish when enqueueing fails.
- Returns the number of currently pending actions.

`getSelectedSlot():number`
Gets the currently selected robot inventory slot. This is used by many modules as an implicit input.
- Returns the index of the selected inventory slot.

`getStackInSlot(slot:number):table`
Gets a description of the item in the specified slot.
- `slot`: the index of the slot to get the item description for.
- Returns a description of the item in the slot.

`getStatusColor():number`
Gets the color of the status light on the front of the robot.
- Returns the current color, as a packed `0xRRGGBB` value.

`getStatusValue():number`
Gets how far the status light is filled.
- Returns the current fill value, in the range of [0, 1].

`move(direction:string):boolean`
Tries to enqueue a movement action in the specified direction.
- `direction`: the direction to move in: `forward`, `backward`, `upward` or `downward`, or a short form.
- Returns whether the action was enqueued successfully.

`setSelectedSlot(slot:number):number`
Sets the currently selected robot inventory slot. This is used by many modules as an implicit input.
- `slot`: the index of the inventory slot to select.
- Returns the index of the newly selected slot. This may differ from `slot` if the specified value was invalid.

`setStatusColor(color:number):number`
Sets the color of the status light on the front of the robot.
- `color`: the color to set, as a packed `0xRRGGBB` value.
- Returns the color that was applied.

`setStatusValue(value:number):number`
Sets how far the status light is filled. It fills from the bottom, so zero hides it entirely and one fills it.
- `value`: the fill value to set, will be clamped to [0, 1].
- Returns the fill value that was applied.

`turn(direction:string):boolean`
Tries to enqueue a turn action towards the specified direction.
- `direction`: the direction to turn towards: `left` or `right`.
- Returns whether the action was enqueued successfully.

## Mid-level API
Device name: `ROBOT`

Directions and sides are numbered. Movement directions are `0` forward, `1` backward, `2` upward and `3` downward. Rotation directions are `0` left and `1` right. Sides for `detect` are `0` front, `1` up and `2` down. Anything outside those ranges fails with `OCEARG`. Item numbers are two bytes, low byte first, as on the `ITEMS` device.

### Methods

`1 detect`
Reports what occupies the space on that side.
- Takes one byte, the side.
- Returns one byte: `0` air, `1` fluid, `2` solid. Only `2` stops a move.

`2 getEnergyStored`
Reads how much energy the robot has left.
- Returns four bytes, the amount, low byte first.

`3 getEnergyCapacity`
Reads how much energy the robot holds when full.
- Returns four bytes, the amount, low byte first.

`4 getSelectedSlot`
Reads which inventory slot is selected.
- Returns one byte, the slot.

`5 setSelectedSlot`
Selects an inventory slot.
- Takes one byte, the slot. A slot the robot does not have is clamped into range.
- Returns one byte, the slot in effect after the call.

`6 getStackInSlot`
Reads what is in an inventory slot.
- Takes one byte, the slot. A slot the robot does not have fails with `OCEARG`.
- Returns four bytes, one slot record in the form the `ITEMS` device uses: the item as two bytes, the number of items, and damage.

`7 getItemName`
Reads the name of an item.
- Takes two bytes, the item id.
- Returns the name, such as `minecraft:cobblestone`. Read while `OCDAV` is set to get all of it.

`8 getItemId`
Looks an item up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the item id.

`9 move`
Enqueues a movement.
- Takes one byte, the direction.
- Returns one byte, `1` when the action was enqueued, `0` when the queue was full or the robot was not ready.

`10 turn`
Enqueues a rotation.
- Takes one byte, the direction.
- Returns one byte, `1` when the action was enqueued, `0` when the queue was full or the robot was not ready.

`11 getLastActionId`
Reads the id of the last enqueued action.
- Returns two bytes, the id, low byte first. Read it right after a `move` or `turn` that returned `1`.

`12 getQueuedActionCount`
Reads how many actions are still waiting.
- Returns one byte, the count.

`13 getActionResult`
Reads how an action turned out. Poll it until it stops reading `1` to wait for an action to finish.
- Takes two bytes, the id.
- Returns one byte: `0` unknown, `1` incomplete, `2` success, `3` failure.

`14 getStatusColor`
Reads the color of the status light.
- Returns three bytes, the red, green and blue components.

`15 setStatusColor`
Sets the color of the status light.
- Takes three bytes, the red, green and blue components.

`16 getStatusValue`
Reads how far the status light is filled.
- Returns one byte, the fill value, where `0` is empty and `255` is full.

`17 setStatusValue`
Sets how far the status light is filled.
- Takes one byte, the fill value, where `0` is empty and `255` is full.
