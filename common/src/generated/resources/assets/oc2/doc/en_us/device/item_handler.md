# Item Handler

## High-level API
Device name: `item_handler`

Provided by any inventory connected through a [bus interface](../block/bus_interface.md), such as a chest. To move items between inventories, use a [transposer](../block/transposer.md).

With several inventories connected, `find` may return any of them. To grab a specific one, give the bus interface in front of the one you want a name with a [wrench](../item/wrench.md), and find it by that name instead.

### Methods

`getItemSlotCount():number`
Gets how many slots the inventory has.
- Returns the number of slots.

`getItemSlotLimit(slot:number):number`
Gets how many items the specified slot can hold.
- `slot`: the number of the slot to look at.
- Returns the most items the slot takes.

`getItemStackInSlot(slot:number):table`
Gets what is in the specified slot.
- `slot`: the number of the slot to look at.
- Returns a table with the item information. Returns nothing for an empty slot.

## Mid-level API
Device name: `ITEMS`

Each inventory is listed separately. When several are connected, pass a count in `B` to `OCFIND` to pick one, or check what `DEVS` lists.

Items are identified by ids to save bus bandwidth. An id is stable within a world, but not across changes to the installed mods. An empty slot reads as item 0. Item numbers are two bytes, low byte first.

A slot the inventory does not have, or a name no item goes by, fails with `OCEARG`.

### Methods

`1 getSlotCount`
Reads how many slots the inventory has.
- Returns one byte, the slot count, at most 255.

`2 getSlots`
Reads a run of slots in one call.
Ask for 1 to 64 slots; more than that fails with `OCEARG`, since the reply would not fit. Reading stops at the end of the inventory, so asking for 64 slots starting at 0 gives you as many as there are. Damage is in [0, 255], where 0 means undamaged, or an item that does not take damage at all, and 255 means about to break.
- Takes two bytes, the slot to start at and how many slots to read.
- Returns four bytes per slot: the item as two bytes, the number of items up to 255, and damage.

`3 getSlotLimit`
Reads how much the slot can hold.
- Takes one byte, the slot.
- Returns one byte, the limit, at most 255.

`4 getItemName`
Reads the name of an item.
- Takes two bytes, the item id.
- Returns the name, such as `minecraft:redstone`. Read while `OCDAV` is set to get all of it.

`5 getItemId`
Looks an item up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the item id.
