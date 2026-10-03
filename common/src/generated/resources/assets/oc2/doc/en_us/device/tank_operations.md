# Tank Operations

## High-level API
Device name: `tank_operations`

Provided by the [tank operations module](../item/tank_operations_module.md) to robots.

The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.

Allows operating the robot's tank, as well as inspecting and operating block and entity tanks. If neither external tank type is present, space permitting, fluids are poured or drained, one bucket at a time.

Sides without a container report no tank, which allows predicting world interaction, e.g. to ensure things are never dumped into the world.

### Methods

`drain(amount:number, [side:string]):number`
Tries to move fluid from a tank in the specified direction into the robot's tank.
- `amount`: the most millibuckets to move.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the amount moved in millibuckets.
- Energy cost: `1`

`fill(amount:number, [side:string]):number`
Tries to move fluid from the robot's tank into a tank in the specified direction.
- `amount`: the most millibuckets to move.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the amount moved in millibuckets.
- Energy cost: `1`

`getFluidInTank(tank:number, [side:string]):table`
Gets what is in the specified tank of the container on the specified side.
- `tank`: the number of the tank to inspect.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns a table with the fluid `id` and the `amount` in millibuckets. Returns nothing for an empty tank.

`getFluidTankCapacity(tank:number, [side:string]):number`
Gets how much the specified tank of the container on the specified side can hold.
- `tank`: the number of the tank to inspect.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the capacity in millibuckets.

`getFluidTankCount([side:string]):number`
Gets how many tanks the fluid container on the specified side has.
- `side`: `front`, `up` or `down`. Optional, defaults to `front`.
- Returns the number of tanks, or `0` if there is no fluid container on that side.

`moveFrom(amount:number):number`
Tries to move fluid from the container item in the selected slot into the robot's tank.
- `amount`: the most millibuckets to move.
- Returns the amount moved in millibuckets.
- Energy cost: `1`

`moveInto(amount:number):number`
Tries to move fluid from the robot's tank into the container item, such as a bucket, in the selected slot.
- `amount`: the most millibuckets to move.
- Returns the amount moved in millibuckets.
- Energy cost: `1`

## Mid-level API
Device name: `TNKOPS`

Sides are numbered: `0` front, `1` up and `2` down. Fluid ids are two bytes, low byte first; [`SYSTEM`](system.md) provides name lookup. Amounts are millibuckets, four bytes, low byte first.

### Methods

`1 fill`
Tries to move fluid from the robot's tank into a tank in the specified direction.
- Takes five bytes: four bytes for the most millibuckets to move, and the side.
- Returns four bytes, the amount moved.
- Energy cost: `1`

`2 drain`
Tries to move fluid from a tank in the specified direction into the robot's tank.
- Takes five bytes: four bytes for the most millibuckets to move, and the side.
- Returns four bytes, the amount moved.
- Energy cost: `1`

`3 moveInto`
Tries to move fluid from the robot's tank into the container item, such as a bucket, in the selected slot.
- Takes four bytes, the most millibuckets to move.
- Returns four bytes, the amount moved.
- Energy cost: `1`

`4 moveFrom`
Tries to move fluid from the container item in the selected slot into the robot's tank.
- Takes four bytes, the most millibuckets to move.
- Returns four bytes, the amount moved.
- Energy cost: `1`

`5 getFluidTankCount`
Reads how many tanks the container on that side has.
- Takes one byte, the side.
- Returns one byte, the tank count, at most 255. A side without a fluid container reads as 0.

`6 getFluidTanks`
Reads a run of tanks of the container on that side.
- Takes three bytes, the side, the tank to start at and how many tanks to read, from 1 to 42.
- Returns six bytes per tank: the fluid as two bytes and the amount as four bytes.

`7 getFluidTankCapacity`
Reads how much a tank of the container on that side can hold.
- Takes two bytes, the side and the tank.
- Returns four bytes, the capacity.
