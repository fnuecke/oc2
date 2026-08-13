# Block Operations Module
![Break it, quick replace it](item:oc2:block_operations_module)

The block operations module provides to [robots](robot.md) the ability to break and place blocks in the world.

## API
Device name: `block_operations`

This is a high level API device. It can be controlled using Lua in the default Linux distribution. For example:  
`local d = require("devices")`  
`local m = d:find("block_operations")`  
`m:excavate("front")`

### Sides
The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.

### Tools
The module swings whatever is in the robot's currently selected inventory slot, exactly as a player holding that item would. A pickaxe mines stone, a shovel digs dirt, an axe chops wood, and an empty slot means bare robot hands.

The tool decides:
- **What can be broken.** Blocks that would not drop anything for the held tool are refused outright, rather than being broken for nothing.
- **How long it takes.** A block that it would take a player five seconds to break also occupies the module for five seconds. Enchantments such as Efficiency, Fortune and Silk Touch all apply.

Note that the tool will take damage and eventually break. Check its durability if you'd rather repair it.

Blocks nothing can break, such as bedrock, and blocks that would take longer than fifteen seconds are refused.

### Methods
`excavate([side]):boolean` tries to break a block in the specified direction using the tool in the currently selected inventory slot. Collected blocks will be inserted starting after the currently selected inventory slot. If a slot is full, the next slot will be used. If the inventory has no space for the dropped block, it will drop into the world.
- `side` is the relative direction in to break a block in. Optional, defaults to `front`. See the "Sides" section.
- Returns whether the operation was successful.

`place([side]):boolean` tries to place a block in the specified direction. Blocks will be placed from the currently selected inventory slot. If the slot is empty, no block will be placed.
- `side` is the relative direction to place the block in. Optional, defaults to `front`. See the "Sides" section.
- Returns whether the operation was successful.

`durability():number` returns the remaining durability of the tool in the currently selected inventory slot.
- Returns the remaining durability, or zero if the slot is empty or holds something that cannot take damage.