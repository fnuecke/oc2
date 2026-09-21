# Inventory Operations

## High-level API
Device name: `inventory_operations`

Provided by the [inventory operations module](../item/inventory_operations_module.md) to robots.

### Sides
The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.

### Methods

`drop(count:number, [side:string]):number`
Tries to drop items from the selected slot in the specified direction. Items are dropped into an inventory, or into the world if no inventory is present.
- `count`: the number of items to drop.
- `side`: the relative direction to drop the items in. Optional, defaults to `front`. One of `front`, `up` or `down`.
- Returns the number of items dropped.

`dropInto(intoSlot:number, count:number, [side:string]):number`
Tries to drop items from the selected slot into the specified slot of an inventory in the specified direction. Items are only dropped into an inventory, never into the world.
- `intoSlot`: the slot to insert the items into.
- `count`: the number of items to drop.
- `side`: the relative direction to drop the items in. Optional, defaults to `front`. One of `front`, `up` or `down`.
- Returns the number of items dropped.

`move(fromSlot:number, intoSlot:number, count:number)`
Tries to move the specified number of items from one robot inventory slot to another.
- `fromSlot`: the slot to extract items from.
- `intoSlot`: the slot to insert items into.
- `count`: the number of items to move.

`take(count:number, [side:string]):number`
Tries to take the specified number of items from the specified direction. Items are taken from an inventory, or from the world if no inventory is present.
- `count`: the number of items to take.
- `side`: the relative direction to take the items from. Optional, defaults to `front`. One of `front`, `up` or `down`.
- Returns the number of items taken.

`takeFrom(fromSlot:number, count:number, [side:string]):number`
Tries to take the specified number of items from the specified slot of an inventory in the specified direction. Items are only taken from an inventory, never from the world.
- `fromSlot`: the slot to take the items from.
- `count`: the number of items to take.
- `side`: the relative direction to take the items from. Optional, defaults to `front`. One of `front`, `up` or `down`.
- Returns the number of items taken.
