# Scanner

## High-level API
Device name: `scanner`

Provided by the [scanner module](../item/scanner_module.md) to robots.

### Sides
The side parameter of `inspect()` represents a face of the robot. Valid values are: `front`, `up` and `down`.

### Scanning
`scan()` measures the hardness of every block in a cube of seven by seven by seven blocks with the robot at its center. Positions use the axes of the robot's `getPosition()`: `x` grows towards east, `y` upward and `z` towards south.

The result holds one byte per block. Blocks are ordered by `y`, then `z`, then `x`, each from `-3` to `3`, so the result is a stack of horizontal layers from bottom to top, and `x` changes fastest. The block at offset `x`, `y`, `z` from the robot is at index `(y + 3) * 7 * 7 + (z + 3) * 7 + (x + 3)`, counting from zero. Since Lua counts from one, this is equivalent to `string.byte(hardness, index + 1)`.

Each byte encodes air as `0`, any fluid as `254` and unbreakable or unscannable blocks as `255`. Regular blocks are encoded as hardness mapped into the `1` to `253` range.

After a scan, the scanner needs five seconds to recharge. Scanning while it recharges fails.

### Fluids
Fluids are always reported by their source fluid, such as `minecraft:water`.

### Entities
Entities are reported by identifier, such as `minecraft:sheep`, closest first. At most twenty are listed.

### Methods

`canSeeSky():boolean`
Checks whether the robot's space gets full sky light.
- Returns whether the sky is visible.

`inspect(side:string):table`
Identifies the block, fluid and entities in the space on the specified side of the robot.
- `side`: the side to look at: `front`, `up` or `down`.
- Returns a table with the `block` and `fluid`, if present, and a list of `entities`.
- Energy cost: `1`

`scan():table`
Measures the hardness of the blocks around the robot and lists nearby entities.
- Returns a table with the block `hardness` as binary data, see above, and the list of `entities` in the scanned cube.
- Energy cost: `10`

## Mid-level API
Device name: `SCANNR`

Sides are numbered: `0` front, `1` up and `2` down. Block numbers are two bytes, low byte first, where `0` is air. Fluid numbers are two bytes, low byte first, as on the `FLUIDS` device, where `0` is no fluid.

A scan is read one layer at a time. Layers are horizontal and numbered `0` to `6` from bottom to top, each holds 49 bytes ordered from north to south, then from west to east. Byte values are as described for the high-level API.

### Methods

`1 inspect`
Identifies the block, fluid and entities in the space on the specified side of the robot.
- Takes one byte, the side.
- Returns five bytes: the block as two bytes, the fluid as two bytes, and the number of entities.
- Energy cost: `1`

`2 scan`
Measures the hardness of the blocks around the robot and lists nearby entities. Read the result with `getScanLayer`.
- Returns one byte, the number of entities in the scanned cube.
- Energy cost: `10`

`3 getScanLayer`
Reads one layer of the last scan. Fails if there was no scan yet.
- Takes one byte, the layer.
- Returns 49 bytes, the hardness of each block in the layer.

`4 getBlockName`
Reads the name of a block.
- Takes two bytes, the block id.
- Returns the name, such as `minecraft:stone`. Read while `OCDAV` is set to read fully.

`5 getBlockId`
Looks a block up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the block id.

`6 canSeeSky`
Checks whether the robot's space gets full sky light.
- Returns one byte, `1` if the sky is visible, `0` otherwise.
