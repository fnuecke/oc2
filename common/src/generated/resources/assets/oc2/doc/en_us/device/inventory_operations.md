# Inventory Operations

## High-level API
Device name: `inventory_operations`

Provided by the [inventory operations module](../item/inventory_operations_module.md) to robots.

The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.

Allows inspecting and operating on the internal inventory, as well as block and entity containers. If neither external container type is present, space permitting, items are dropped into the world or picked up.

Sides without a container report no inventory, which allows predicting world interaction, e.g. to ensure things are never dumped into the world.

### Methods

`drop(count:number, [side:string]):number`
Tries to drop items from the selected slot in the specified direction. Items are dropped into an inventory, or into the world if no inventory is present.
- `count`: the number of items to drop.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of items dropped.

`dropInto(intoSlot:number, count:number, [side:string]):number`
Tries to drop items from the selected slot into the specified slot of an inventory in the specified direction. Items are only dropped into an inventory, never into the world.
- `intoSlot`: the slot to insert items into.
- `count`: the number of items to drop.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of items dropped.

`getItemSlotCount([side:string]):number`
Gets how many slots the inventory on the specified side has.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of slots, or `0` if there is no inventory on that side.

`getItemSlotLimit(slot:number, [side:string]):number`
Gets how many items the specified slot of the inventory on the specified side can hold.
- `slot`: the slot to inspect.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the most items the slot takes.

`getItemStackInSlot(slot:number, [side:string]):table`
Gets what is in the specified slot of the inventory on the specified side.
- `slot`: the slot to inspect.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns a table with the item information. Returns nothing for an empty slot.

`move(fromSlot:number, intoSlot:number, count:number):number`
Tries to move the specified number of items from one robot inventory slot to another.
- `fromSlot`: the slot to extract items from.
- `intoSlot`: the slot to insert items into.
- `count`: the number of items to move.
- Returns the number of items moved.

`moveFrom(intoSlot:number, count:number):number`
Tries to move items out of a container item in the selected slot into the specified robot inventory slot.
- `intoSlot`: the slot to insert items into.
- `count`: the number of items to move.
- Returns the number of items moved.

`moveInto(fromSlot:number, count:number):number`
Tries to move items from the specified robot inventory slot into a container item, such as a shulker box, in the selected slot.
- `fromSlot`: the slot to take items from.
- `count`: the number of items to move.
- Returns the number of items moved.

`take(count:number, [side:string]):number`
Tries to take the specified number of items from the specified direction. Items are taken from an inventory, or from the world if no inventory is present.
- `count`: the number of items to take.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of items taken.

`takeFrom(fromSlot:number, count:number, [side:string]):number`
Tries to take the specified number of items from the specified slot of an inventory in the specified direction. Items are only taken from an inventory, never from the world.
- `fromSlot`: the slot to take items from.
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
- Returns one byte, the number of items moved.

`2 drop`
Tries to drop items from the selected slot in the specified direction. Items are dropped into an inventory, or into the world if no inventory is present.
- Takes two bytes: the number of items to drop, and the side to drop them in.
- Returns one byte, the number of items dropped.

`3 dropInto`
Tries to drop items from the selected slot into the specified slot of an inventory in the specified direction. Items are only dropped into an inventory, never into the world.
- Takes three bytes: the slot to insert items into, the number of items to drop, and the side of the inventory.
- Returns one byte, the number of items dropped.

`4 take`
Tries to take the specified number of items from the specified direction. Items are taken from an inventory, or from the world if no inventory is present.
- Takes two bytes: the number of items to take, and the side to take them from.
- Returns one byte, the number of items taken.

`5 takeFrom`
Tries to take the specified number of items from the specified slot of an inventory in the specified direction. Items are only taken from an inventory, never from the world.
- Takes three bytes: the slot to take items from, the number of items to take, and the side of the inventory.
- Returns one byte, the number of items taken.

`6 moveInto`
Tries to move items from the specified robot inventory slot into a container item, such as a shulker box, in the selected slot.
- Takes two bytes: the slot to extract items from, and the number of items to move.
- Returns one byte, the number of items moved.

`7 moveFrom`
Tries to move items out of a container item in the selected slot into the specified robot inventory slot.
- Takes two bytes: the slot to insert items into, and the number of items to move.
- Returns one byte, the number of items moved.

`8 getItemSlotCount`
Reads how many slots the inventory on that side has.
- Takes one byte, the side.
- Returns one byte, the slot count, at most 255. A side without an inventory reads as 0.

`9 getItemSlots`
Reads a run of slots of the inventory on that side.
- Takes three bytes, the side, the slot to start at and how many slots to read, from 1 to 64.
- Returns four bytes per slot: the item as two bytes, the number of items up to 255, and damage.

`10 getItemSlotLimit`
Reads how much a slot of the inventory on that side can hold.
- Takes two bytes, the side and the slot.
- Returns one byte, the limit, at most 255.
