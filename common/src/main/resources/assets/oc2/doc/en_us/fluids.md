# Fluids
Any tank can be inspected through a [bus interface](block/bus_interface.md). This includes cauldrons and the tanks of other mods. To move fluids between tanks, use a [transposer](block/transposer.md). Amounts are in millibuckets, a bucket being 1000.

## High-level API
Device name: `fluid_handler`

For example, to see what is in a connected cauldron:  
`local d = require("devices")`  
`local tank = d:find("fluid_handler")`  
`local fluid = tank:getFluidInTank(0)`  
`print(fluid and fluid.id, fluid and fluid.amount)`

With several tanks connected, `find` may return any of them. To grab a specific one, give the bus interface in front of the one you want a name with a [wrench](item/wrench.md), and find it by that name instead.

### Methods
`getFluidTankCount():number` gets how many tanks the container has.
- Returns the number of tanks.

`getFluidInTank(tank:number):table` gets what is in the specified tank.
- `tank` is the number of the tank to look at.
- Returns a table with the fluid `id`, such as `minecraft:water`, and the `amount` in millibuckets. Returns nothing for an empty tank.

`getFluidTankCapacity(tank:number):number` gets how much the specified tank can hold.
- `tank` is the number of the tank to look at.
- Returns the capacity in millibuckets.

## Mid-level API
Device name: `FLUIDS`

Each container is listed separately. When several are connected, pass a count in `B` to `OCFIND` to pick one, or check what `DEVS` lists.

Fluids are identified by ids to save bus bandwidth. An id is stable within a world, but not across changes to the installed mods. An empty tank reads as fluid 0. Fluid numbers are two bytes, low byte first. Amounts are four bytes, low byte first.

`1 getTankCount()` reads how many tanks the container has.
- Returns one byte, the tank count, at most 255.

`2 getTanks(first, count)` reads a run of tanks in one call.
- Takes two bytes, the tank to start at and how many tanks to read.
- Returns six bytes per tank: the fluid as two bytes and the amount as four bytes.

Ask for 1 to 42 tanks; more than that fails with `OCEARG`, since the reply would not fit. Reading stops at the end of the container, so asking for 42 tanks starting at 0 gives you as many as there are.

`3 getTankCapacity(tank)` reads how much the tank can hold.
- Takes one byte, the tank.
- Returns four bytes, the capacity.

`4 getFluidName(fluid)` reads the name of a fluid.
- Takes two bytes, the fluid id.
- Returns the name, such as `minecraft:water`. Read while `OCDAV` is set to get all of it.

`5 getFluidId(name)` looks a fluid up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the fluid id.

A tank the container does not have, or a name no fluid goes by, fails with `OCEARG`.
