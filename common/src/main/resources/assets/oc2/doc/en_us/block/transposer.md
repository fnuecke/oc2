# Transposer
![Less posing, more rights](block:oc2:transposer)

The transposer moves items and fluids between containers next to it. This includes chests, cauldrons and machines supporting automation.

It can be controlled using both the [high-level API](../hlapi.md) and the [mid-level API](../mlapi.md). For example:  
`local d = require("devices")`  
`local t = d:find("transposer")`  
`print(t:moveItems("north", 0,`  
`                  "south", 0, 64))`

## High-level API
Device name: `transposer`

### Methods
`getItemSlotCount(side):number` gets how many slots the inventory on the specified side has.
- `side` is the side of the inventory to inspect.
- Returns the number of slots, or `0` if there is no inventory on that side.

`getItemStackInSlot(side, slot:number):table` gets what is in the specified slot of the inventory on the specified side.
- `side` is the side of the inventory to inspect.
- `slot` is the number of the slot to look at.
- Returns a table with the item information. Returns nothing for an empty slot.

`getItemSlotLimit(side, slot:number):number` gets how many items the specified slot of the inventory on the specified side can hold.
- `side` is the side of the inventory to inspect.
- `slot` is the number of the slot to look at.
- Returns the most items the slot takes.

`moveItems(sourceSide, sourceSlot:number, targetSide, targetSlot:number, count:number):number` moves items from one inventory to another. It moves as many items as the source slot yields and the target slot accepts, up to `count`.
- `sourceSide` is the side of the inventory to take items from.
- `sourceSlot` is the number of the slot to take items from.
- `targetSide` is the side of the inventory to put items into.
- `targetSlot` is the number of the slot to put items into.
- `count` is the most items to move.
- Returns the number of items moved.

`getFluidTankCount(side):number` gets how many tanks the fluid container on the specified side has.
- `side` is the side of the container to inspect.
- Returns the number of tanks, or `0` if there is no fluid container on that side.

`getFluidInTank(side, tank:number):table` gets what is in the specified tank of the container on the specified side.
- `side` is the side of the container to inspect.
- `tank` is the number of the tank to look at.
- Returns a table with the fluid `id` and the `amount` in millibuckets. Returns nothing for an empty tank.

`getFluidTankCapacity(side, tank:number):number` gets how much the specified tank of the container on the specified side can hold.
- `side` is the side of the container to inspect.
- `tank` is the number of the tank to look at.
- Returns the capacity in millibuckets.

`moveFluid(sourceSide, targetSide, amount:number):number` moves fluid from one container to another. It moves as much as the source yields and the target accepts, up to `amount`. A bucket is 1000.
- `sourceSide` is the side of the container to drain.
- `targetSide` is the side of the container to fill.
- `amount` is the most millibuckets to move.
- Returns the amount moved in millibuckets.

## Mid-level API
Device name: `TRANSP`

`1 getSlotCount(side)` reads how many slots the inventory on that side has.
- Takes one byte, the side.
- Returns one byte, the slot count, at most 255. A side without an inventory reads as 0.

`2 getSlots(side, first, count)` reads a run of slots of the inventory on that side.
- Takes three bytes, the side, the slot to start at and how many slots to read, from 1 to 64.
- Returns four bytes per slot: the item as two bytes, the number of items up to 255, and damage.

`3 getSlotLimit(side, slot)` reads how much a slot of the inventory on that side can hold.
- Takes two bytes, the side and the slot.
- Returns one byte, the limit, at most 255.

`4 getItemName(item)` reads the name of an item.
- Takes two bytes, the item id.
- Returns the name, such as `minecraft:redstone`. Read while `OCDAV` is set to get all of it.

`5 getItemId(name)` looks an item up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the item id.

`6 moveItems(sourceSide, sourceSlot, targetSide, targetSlot, count)` moves up to `count` items between two slots.
- Takes five bytes, the side and slot to take from, the side and slot to put into, and how many items to move at most.
- Returns one byte, how many items were moved.

Fluids follow the same pattern. Amounts are millibuckets, four bytes, low byte first, as on the `FLUIDS` device.

`7 getTankCount(side)` reads how many tanks the container on that side has.
- Takes one byte, the side.
- Returns one byte, the tank count, at most 255. A side without a fluid container reads as 0.

`8 getTanks(side, first, count)` reads a run of tanks of the container on that side.
- Takes three bytes, the side, the tank to start at and how many tanks to read, from 1 to 42.
- Returns six bytes per tank: the fluid as two bytes and the amount as four bytes.

`9 getTankCapacity(side, tank)` reads how much a tank of the container on that side can hold.
- Takes two bytes, the side and the tank.
- Returns four bytes, the capacity.

`10 getFluidName(fluid)` reads the name of a fluid.
- Takes two bytes, the fluid id.
- Returns the name, such as `minecraft:water`. Read while `OCDAV` is set to get all of it.

`11 getFluidId(name)` looks a fluid up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the fluid id.

`12 moveFluid(sourceSide, targetSide, amount)` moves up to `amount` millibuckets between two containers.
- Takes six bytes, the side to drain, the side to fill, and four bytes for how much to move at most.
- Returns four bytes, how much was moved.
