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
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of items dropped.

`dropInto(intoSlot:number, count:number, [side:string]):number`
Tries to drop items from the selected slot into the specified slot of an inventory in the specified direction. Items are only dropped into an inventory, never into the world.
- `intoSlot`: the slot to insert the items into.
- `count`: the number of items to drop.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of items dropped.

`move(fromSlot:number, intoSlot:number, count:number)`
Tries to move the specified number of items from one robot inventory slot to another.
- `fromSlot`: the slot to extract items from.
- `intoSlot`: the slot to insert items into.
- `count`: the number of items to move.

`take(count:number, [side:string]):number`
Tries to take the specified number of items from the specified direction. Items are taken from an inventory, or from the world if no inventory is present.
- `count`: the number of items to take.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of items taken.

`takeFrom(fromSlot:number, count:number, [side:string]):number`
Tries to take the specified number of items from the specified slot of an inventory in the specified direction. Items are only taken from an inventory, never from the world.
- `fromSlot`: the slot to take the items from.
- `count`: the number of items to take.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of items taken.

## Mid-level API
Device name: `INVOPS`

Sides are numbered: `0` front, `1` up and `2` down.

### Methods

`1 move`
Tries to move the specified number of items from one robot inventory slot to another.
- Takes three bytes, the slot to extract items from, the slot to insert items into, and the number of items to move.

`2 drop`
Tries to drop items from the selected slot in the specified direction. Items are dropped into an inventory, or into the world if no inventory is present.
- Takes two bytes: the number of items to drop, and the side to drop them in.
- Returns one byte, the number of items dropped.

`3 dropInto`
Tries to drop items from the selected slot into the specified slot of an inventory in the specified direction. Items are only dropped into an inventory, never into the world.
- Takes three bytes: the slot to insert the items into, the number of items to drop, and the side of the inventory.
- Returns one byte, the number of items dropped.

`4 take`
Tries to take the specified number of items from the specified direction. Items are taken from an inventory, or from the world if no inventory is present.
- Takes two bytes: the number of items to take, and the side to take them from.
- Returns one byte, the number of items taken.

`5 takeFrom`
Tries to take the specified number of items from the specified slot of an inventory in the specified direction. Items are only taken from an inventory, never from the world.
- Takes three bytes: the slot to take the items from, the number of items to take, and the side of the inventory.
- Returns one byte, the number of items taken.
