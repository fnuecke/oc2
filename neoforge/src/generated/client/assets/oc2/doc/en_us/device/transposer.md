# Transposer

## High-level API
Device name: `transposer`

Provided by the [transposer](../block/transposer.md) block.

Allows inspecting and operating on block and entity containers. If neither are present, space permitting, items are dropped into the world or picked up, fluids are poured or drained, one bucket at a time.

Sides without a container report no inventory or tank, which allows predicting world interaction, e.g. to ensure things are never dumped into the world.

### Methods

`getFluidInTank(side:string, tank:number):table`
Gets what is in the specified tank of the container on the specified side.
- `side`: the side of the container to inspect.
- `tank`: the number of the tank to inspect.
- Returns a table with the fluid `id` and the `amount` in millibuckets. Returns nothing for an empty tank.

`getFluidTankCapacity(side:string, tank:number):number`
Gets how much the specified tank of the container on the specified side can hold.
- `side`: the side of the container to inspect.
- `tank`: the number of the tank to inspect.
- Returns the capacity in millibuckets.

`getFluidTankCount(side:string):number`
Gets how many tanks the fluid container on the specified side has.
- `side`: the side of the container to inspect.
- Returns the number of tanks, or `0` if there is no fluid container on that side.

`getItemSlotCount(side:string):number`
Gets how many slots the inventory on the specified side has.
- `side`: the side of the inventory to inspect.
- Returns the number of slots, or `0` if there is no inventory on that side.

`getItemSlotLimit(side:string, slot:number):number`
Gets how many items the specified slot of the inventory on the specified side can hold.
- `side`: the side of the inventory to inspect.
- `slot`: the number of the slot to inspect.
- Returns the most items the slot takes.

`getItemStackInSlot(side:string, slot:number):table`
Gets what is in the specified slot of the inventory on the specified side.
- `side`: the side of the inventory to inspect.
- `slot`: the number of the slot to inspect.
- Returns a table with the item information. Returns nothing for an empty slot.

`moveFluid(sourceSide:string, targetSide:string, amount:number):number`
Moves fluid from one container to another. It moves as much as the source yields and the target accepts, up to `amount`. A bucket is 1000.
- `sourceSide`: the side of the container to drain.
- `targetSide`: the side of the container to fill.
- `amount`: maximum amount to move, in millibuckets.
- Returns the amount transferred in millibuckets.
- Energy cost: `1`

`moveItems(sourceSide:string, targetSide:string, count:number):number`
Moves items from one inventory to another, from and into any slot. It moves as many items as the first source slot that yields anything gives and the first target slot that accepts anything takes, up to `count`.
- `sourceSide`: the side of the inventory to take items from.
- `targetSide`: the side of the inventory to put items into.
- `count`: the most items to move.
- Returns the number of items transferred.
- Energy cost: `1`

`moveItems(sourceSide:string, sourceSlot:number, targetSide:string, targetSlot:number, count:number):number`
Moves items from one inventory slot to another. It moves as many items as the source slot yields and the target slot accepts, up to `count`. Only uses inventories, never open space.
- `sourceSide`: the side of the inventory to take items from.
- `sourceSlot`: the number of the slot to take items from.
- `targetSide`: the side of the inventory to put items into.
- `targetSlot`: the number of the slot to put items into.
- `count`: the most items to move.
- Returns the number of items transferred.
- Energy cost: `1`

## Mid-level API
Device name: `TRANSP`

Sides are numbered as in the "Sides" section above. Item and fluid ids are two bytes, low byte first; [`SYSTEM`](system.md) provides name lookup. Amounts are millibuckets, four bytes, low byte first.

A side with neither a container nor available space, or a protected side, fails with `OCEARG`.

### Methods

`1 getItemSlotCount`
Reads how many slots the inventory on that side has.
- Takes one byte, the side.
- Returns one byte, the slot count, at most 255. A side without an inventory reads as 0.

`2 getItemSlots`
Reads a run of slots of the inventory on that side.
- Takes three bytes, the side, the slot to start at and how many slots to read, from 1 to 64.
- Returns four bytes per slot: the item as two bytes, the number of items up to 255, and damage.

`3 getItemSlotLimit`
Reads how much a slot of the inventory on that side can hold.
- Takes two bytes, the side and the slot.
- Returns one byte, the limit, at most 255.

`4 moveItems`
Moves up to `count` items between two slots.
- Takes five bytes, the side and slot to take from, the side and slot to put into, and how many items to move at most.
- Returns one byte, how many items were moved.
- Energy cost: `1`

`5 moveItemsAny`
Moves up to `count` items from any slot into any slot.
- Takes three bytes, the side to take from, the side to put into, and how many items to move at most.
- Returns one byte, how many items were moved.
- Energy cost: `1`

`6 getFluidTankCount`
Reads how many tanks the container on that side has.
- Takes one byte, the side.
- Returns one byte, the tank count, at most 255. A side without a fluid container reads as 0.

`7 getFluidTanks`
Reads a run of tanks of the container on that side.
- Takes three bytes, the side, the tank to start at and how many tanks to read, from 1 to 42.
- Returns six bytes per tank: the fluid as two bytes and the amount as four bytes.

`8 getFluidTankCapacity`
Reads how much a tank of the container on that side can hold.
- Takes two bytes, the side and the tank.
- Returns four bytes, the capacity.

`9 moveFluid`
Moves up to `amount` millibuckets between two containers.
- Takes six bytes, the side to drain, the side to fill, and four bytes for how much to move at most.
- Returns four bytes, how much was moved.
- Energy cost: `1`
