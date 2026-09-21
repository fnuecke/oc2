# Transposer

## High-level API
Device name: `transposer`

Provided by the [transposer](../block/transposer.md) block.

### Sides
Sides name the container next to the transposer. Relative sides turn with the block: `front`, `back`, `left` and `right`, seen when looking at the primary face, the one with a single marking. Absolute sides always mean the same direction in the world: `north`, `south`, `west` and `east`. `up` and `down` mean the same thing either way.

Sides may also be given as a number instead of a name. Numbers are relative: `0` is `down`, `1` is `up`, `2` is `back`, `3` is `front`, `4` is `left` and `5` is `right`.

Sides that are protected, for example by spawn protection, fail with an error.

### Methods

`drainFluid(sourceSide:string, targetSide:string):number`
Drains a fluid source block, or out of a waterlogged block, into a container. Nothing is transferred if the container cannot hold a full bucket of it.
- `sourceSide`: the side to take the fluid from.
- `targetSide`: the side of the container to fill.
- Returns the amount transferred in millibuckets, `1000` or `0`.

`dropItems(sourceSide:string, sourceSlot:number, targetSide:string, count:number):number`
Drops items from an inventory into the world. The target side must not be blocked.
- `sourceSide`: the side of the inventory to take items from.
- `sourceSlot`: the number of the slot to take items from.
- `targetSide`: the side to drop the items on.
- `count`: the most items to drop.
- Returns the number of items transferred.

`fillFluid(sourceSide:string, targetSide:string):number`
Pours one bucket of fluid from a container into the world. Nothing is transferred if the container holds less than a bucket, or the fluid cannot go there. Water placed in the Nether evaporates and still counts as placed.
- `sourceSide`: the side of the container to drain.
- `targetSide`: the side to place the fluid on.
- Returns the amount transferred in millibuckets, `1000` or `0`.

`getFluidInTank(side:string, tank:number):table`
Gets what is in the specified tank of the container on the specified side.
- `side`: the side of the container to inspect.
- `tank`: the number of the tank to look at.
- Returns a table with the fluid `id` and the `amount` in millibuckets. Returns nothing for an empty tank.

`getFluidTankCapacity(side:string, tank:number):number`
Gets how much the specified tank of the container on the specified side can hold.
- `side`: the side of the container to inspect.
- `tank`: the number of the tank to look at.
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
- `slot`: the number of the slot to look at.
- Returns the most items the slot takes.

`getItemStackInSlot(side:string, slot:number):table`
Gets what is in the specified slot of the inventory on the specified side.
- `side`: the side of the inventory to inspect.
- `slot`: the number of the slot to look at.
- Returns a table with the item information. Returns nothing for an empty slot.

`moveFluid(sourceSide:string, targetSide:string, amount:number):number`
Moves fluid from one container to another. It moves as much as the source yields and the target accepts, up to `amount`. A bucket is 1000.
- `sourceSide`: the side of the container to drain.
- `targetSide`: the side of the container to fill.
- `amount`: the most millibuckets to move.
- Returns the amount transferred in millibuckets.

`moveItems(sourceSide:string, sourceSlot:number, targetSide:string, targetSlot:number, count:number):number`
Moves items from one inventory to another. It moves as many items as the source slot yields and the target slot accepts, up to `count`.
- `sourceSide`: the side of the inventory to take items from.
- `sourceSlot`: the number of the slot to take items from.
- `targetSide`: the side of the inventory to put items into.
- `targetSlot`: the number of the slot to put items into.
- `count`: the most items to move.
- Returns the number of items transferred.

`takeItems(sourceSide:string, targetSide:string, targetSlot:number, count:number):number`
Picks up items lying in the world into an inventory. It takes as many items as the target slot accepts, up to `count`.
- `sourceSide`: the side to pick items up from.
- `targetSide`: the side of the inventory to put items into.
- `targetSlot`: the number of the slot to put items into.
- `count`: the most items to take.
- Returns the number of items transferred.

## Mid-level API
Device name: `TRANSP`

Sides are numbered as in the "Sides" section above. Item numbers are two bytes, low byte first, as on the `ITEMS` device. Fluids follow the same pattern; amounts are millibuckets, four bytes, low byte first, as on the `FLUIDS` device.

A blocked target side, or a protected side, fails with `OCEARG`.

### Methods

`1 getItemSlotCount`
Reads how many slots the inventory on that side has.
- Takes one byte, the side.
- Returns one byte, the slot count, at most 255. A side without an inventory reads as 0.

`2 getSlots`
Reads a run of slots of the inventory on that side.
- Takes three bytes, the side, the slot to start at and how many slots to read, from 1 to 64.
- Returns four bytes per slot: the item as two bytes, the number of items up to 255, and damage.

`3 getItemSlotLimit`
Reads how much a slot of the inventory on that side can hold.
- Takes two bytes, the side and the slot.
- Returns one byte, the limit, at most 255.

`4 getItemName`
Reads the name of an item.
- Takes two bytes, the item id.
- Returns the name, such as `minecraft:redstone`. Read while `OCDAV` is set to get all of it.

`5 getItemId`
Looks an item up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the item id.

`6 moveItems`
Moves up to `count` items between two slots.
- Takes five bytes, the side and slot to take from, the side and slot to put into, and how many items to move at most.
- Returns one byte, how many items were moved.

`7 getFluidTankCount`
Reads how many tanks the container on that side has.
- Takes one byte, the side.
- Returns one byte, the tank count, at most 255. A side without a fluid container reads as 0.

`8 getTanks`
Reads a run of tanks of the container on that side.
- Takes three bytes, the side, the tank to start at and how many tanks to read, from 1 to 42.
- Returns six bytes per tank: the fluid as two bytes and the amount as four bytes.

`9 getFluidTankCapacity`
Reads how much a tank of the container on that side can hold.
- Takes two bytes, the side and the tank.
- Returns four bytes, the capacity.

`10 getFluidName`
Reads the name of a fluid.
- Takes two bytes, the fluid id.
- Returns the name, such as `minecraft:water`. Read while `OCDAV` is set to get all of it.

`11 getFluidId`
Looks a fluid up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the fluid id.

`12 moveFluid`
Moves up to `amount` millibuckets between two containers.
- Takes six bytes, the side to drain, the side to fill, and four bytes for how much to move at most.
- Returns four bytes, how much was moved.

`13 dropItems`
Drops up to `count` items from a slot into the world.
- Takes four bytes, the side and slot to take from, the side to drop on, and how many items to drop at most.
- Returns one byte, how many items were dropped.

`14 takeItems`
Picks up to `count` items from the world into a slot.
- Takes four bytes, the side to pick up from, the side and slot to put into, and how many items to take at most.
- Returns one byte, how many items were taken.

`15 fillFluid`
Places one bucket of fluid from a container into the world.
- Takes two bytes, the side to drain and the side to place on.
- Returns four bytes, how much was placed, 1000 or 0.

`16 drainFluid`
Takes a fluid block from the world into a container.
- Takes two bytes, the side to take from and the side to fill.
- Returns four bytes, how much was taken, 1000 or 0.
