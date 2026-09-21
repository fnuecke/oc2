# Block Operations

## High-level API
Device name: `block_operations`

Provided by the [block operations module](../item/block_operations_module.md) to robots.

### Sides
The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.

### Tools
The module swings whatever is in the robot's currently selected inventory slot, exactly as a player holding that item would. A pickaxe mines stone, a shovel digs dirt, an axe chops wood, and an empty slot means no tool.

The tool controls what can be broken and how long it takes to break it.

Note that the tool will take damage and eventually break. Check its durability regularly if you'd rather repair it.

Unbreakable blocks, such as bedrock, and blocks that would take longer than fifteen seconds cannot be broken.

### Methods

`durability():number`
Gets the remaining durability of the tool in the currently selected inventory slot.
- Returns the remaining durability, or zero if the slot is empty or holds something that cannot take damage.

`excavate([side:string]):boolean`
Tries to break a block in the specified direction using the tool in the currently selected inventory slot. Collected blocks will be inserted starting after the currently selected inventory slot. If a slot is full, the next slot will be used. If the inventory has no space for the dropped block, it will drop into the world.
- `side`: the relative direction to break a block in. Optional, defaults to `front`. One of `front`, `up` or `down`.
- Returns whether the operation was successful.

`place([side:string]):boolean`
Tries to place a block in the specified direction. Blocks will be placed from the currently selected inventory slot. If the slot is empty, no block will be placed.
- `side`: the relative direction to place the block in. Optional, defaults to `front`. One of `front`, `up` or `down`.
- Returns whether the operation was successful.
