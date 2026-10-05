# Crafting

## High-level API
Device name: `crafting`

Provided by the [crafting module](../item/crafting_module.md) to robots.

The top part of the robot's inventory, slots `0` to `8`, serves as the crafting grid. Crafted items go into the bottom row, starting at the selected slot.

### Methods

`craft():number`
Tries to craft once from the crafting grid. Fails if the grid matches no recipe or the result does not fit into the bottom row.
- Returns the number of items crafted, or `0` if crafting failed.
- Energy cost: `1`

## Mid-level API
Device name: `CRFTNG`

### Methods

`1 craft`
Tries to craft once from the crafting grid. Fails if the grid matches no recipe or the result does not fit into the bottom row.
- Returns one byte, the number of items crafted, `0` if crafting failed.
- Energy cost: `1`
