# Fluid Handler

## High-level API
Device name: `fluid_handler`

Provided by any tank connected through a [bus interface](../block/bus_interface.md), including cauldrons and the tanks of other mods. To move fluids between tanks, use a [transposer](../block/transposer.md). Amounts are in millibuckets, a bucket being 1000.

With several tanks connected, `find` may return any of them. To grab a specific one, give the bus interface in front of the one you want a name with a [wrench](../item/wrench.md), and find it by that name instead.

### Methods

`getFluidInTank(tank:number):table`
Gets what is in the specified tank.
- `tank`: the number of the tank to look at.
- Returns a table with the fluid `id`, such as `minecraft:water`, and the `amount` in millibuckets. Returns nothing for an empty tank.

`getFluidTankCapacity(tank:number):number`
Gets how much the specified tank can hold.
- `tank`: the number of the tank to look at.
- Returns the capacity in millibuckets.

`getFluidTankCount():number`
Gets how many tanks the container has.
- Returns the number of tanks.

## Mid-level API
Device name: `FLUIDS`

Each container is listed separately. When several are connected, pass a count in `B` to `OCFIND` to pick one, or check what `DEVS` lists.

Fluids are identified by ids to save bus bandwidth. An id is stable within a world, but not across changes to the installed mods. An empty tank reads as fluid 0. Fluid numbers are two bytes, low byte first. Amounts are four bytes, low byte first.

A tank the container does not have, or a name no fluid goes by, fails with `OCEARG`.

### Methods

`1 getTankCount`
Reads how many tanks the container has.
- Returns one byte, the tank count, at most 255.

`2 getTanks`
Reads a run of tanks in one call.
Ask for 1 to 42 tanks; more than that fails with `OCEARG`, since the reply would not fit. Reading stops at the end of the container, so asking for 42 tanks starting at 0 gives you as many as there are.
- Takes two bytes, the tank to start at and how many tanks to read.
- Returns six bytes per tank: the fluid as two bytes and the amount as four bytes.

`3 getTankCapacity`
Reads how much the tank can hold.
- Takes one byte, the tank.
- Returns four bytes, the capacity.

`4 getFluidName`
Reads the name of a fluid.
- Takes two bytes, the fluid id.
- Returns the name, such as `minecraft:water`. Read while `OCDAV` is set to get all of it.

`5 getFluidId`
Looks a fluid up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the fluid id.
