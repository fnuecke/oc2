# Crafter

## High-level API
Device name: `crafter`

Provided by crafters connected to a [bus interface](../block/bus_interface.md). The grid slots are available through the [item handler](item_handler.md) device.

### Methods

`getResult():table`
Gets what the crafter would currently craft.
- Returns a table with the item information. Returns nothing if the grid matches no recipe.

`isSlotEnabled(slot:number):boolean`
Gets whether a slot of the grid is enabled.
- `slot`: the slot index.
- Returns `true` if the slot is enabled.

`setSlotEnabled(slot:number, enabled:boolean)`
Sets whether a grid slot is enabled. Slot must be empty to be disabled.
- `slot`: the slot index.
- `enabled`: whether to enable the slot.

## Mid-level API
Device name: `CRAFTR`

Item ids are two bytes, low byte first; [`SYSTEM`](system.md) provides name lookup.

### Methods

`1 isSlotEnabled`
Gets whether a slot of the grid is enabled.
- Takes one byte, the slot index.
- Returns one byte, `1` if enabled, else `0`.

`2 setSlotEnabled`
Sets whether a grid slot is enabled. Slot must be empty to be disabled.
- Takes two bytes, the slot index, and `0` to disable or `1` to enable it.

`3 getResult`
Gets what the crafter would currently craft.
- Returns three bytes: the item as two bytes, `0` if the grid matches no recipe, else the number of items.
