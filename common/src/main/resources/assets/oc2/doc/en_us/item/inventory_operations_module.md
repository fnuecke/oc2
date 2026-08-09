# Inventory Operations Module
![What's yours is mine](item:oc2:inventory_operations_module)

The inventory operations module provides to [robots](robot.md) the ability to insert and extract items from inventories in the world. This supports both block and entity inventories.

## API
Device name: `inventory_operations`

This is a high level API device. It can be controlled using Lua in the default Linux distribution. For example:  
`local d = require("devices")`  
`local m = d:find("inventory_operations")`  
`m:drop(1, "front")`

### Sides
The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.

### Methods
`move(fromSlot:number,intoSlot:number,count:number)` tries to move the specified number of items from one robot inventory slot to another.
- `fromSlot` is the slot to extract items from.
- `intoSlot` is the slot to insert items into.
- `count` is the number of items to move.

`drop(count:number[,side]):number` tries to drop items from the specified slot in the specified direction. It will drop items either into an inventory, or the world if no inventory is present.
- `count` is the number of items to drop.
- `side` is the relative direction to drop the items in. Optional, defaults to `front`. See the "Sides" section.
- Returns the number of items dropped.

`dropInto(intoSlot:number,count:number[,side]):number` tries to drop items from the specified slot into the specified slot of an inventory in the specified direction. It will only drop items into an inventory.
- `intoSlot` is the slot to insert the items into.
- `count` is the number of items to drop.
- `side` is the relative direction to drop the items in. Optional, defaults to `front`. See the "Sides" section.
- Returns the number of items dropped.

`take(count:number[,side]):number` tries to take the specified number of items from the specified direction. It will take items from either an inventory, or the world if no inventory is present.
- `count` is the number of items to take.
- `side` is the relative direction to take the items from. Optional, defaults to `front`. See the "Sides" section.
- Returns the number of items taken.

`takeFrom(fromSlot:number,count:number[,side]):number` tries to take the specified number of items from the specified slot from an inventory in the specified direction. It will only take items from an inventory.
- `fromSlot` is the slot to take the items from.
- `count` is the number of items to take.
- `side` is the relative direction to take the items from. Optional, defaults to `front`. See the "Sides" section.
- Returns the number of items taken.