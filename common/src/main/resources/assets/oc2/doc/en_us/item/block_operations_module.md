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

### Methods
`excavate([side]):boolean` tries to break a block in the specified direction. Collected blocks will be inserted starting at the currently selected inventory slot. If the selected slot is full, the next slot will be used, and so on. If the inventory has no space for the dropped block, it will drop into the world.
- `side` is the relative direction in to break a block in. Optional, defaults to `front`. See the "Sides" section.
- Returns whether the operation was successful.

`place([side]):boolean` tries to place a block in the specified direction. Blocks will be placed from the currently selected inventory slot. If the slot is empty, no block will be placed.
- `side` is the relative direction to place the block in. Optional, defaults to `front`. See the "Sides" section.
- Returns whether the operation was successful.

`durability():number` returns the remaining durability of the module's excavation tool. Once the durability has reached zero, no further excavation operations can be performed until it is repaired.
- Returns the remaining durability of the module's excavation tool

`repair():boolean` attempts to repair the module's excavation tool using materials in the currently selected inventory slot. This method will consume one item at a time. Any regular tool may act as the source for repair materials, such as pickaxes and shovels. The quality of the tool directly effects the amount of durability restored.
- Returns whether some material could be used to repair the module's excavation tool.