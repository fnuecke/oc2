# System

## Mid-level API
Device name: `SYSTEM`

Provided by every computer and robot, always as MLAPI-device index 0.

Items, fluids and blocks are identified by ids to save bus bandwidth. An id is stable within a world, but not across changes to the installed mods. Ids are two bytes, low byte first.

An unknown id or name fails with `OCEARG`.

### Methods

`1 getItemName`
Reads the name of an item.
- Takes two bytes, the item id.
- Returns the name, such as `minecraft:redstone`. Read while `OCDAV` is set to read fully.

`2 getItemId`
Looks an item up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the item id.

`3 getFluidName`
Reads the name of a fluid.
- Takes two bytes, the fluid id.
- Returns the name, such as `minecraft:water`. Read while `OCDAV` is set to read fully.

`4 getFluidId`
Looks a fluid up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the fluid id.

`5 getBlockName`
Reads the name of a block.
- Takes two bytes, the block id.
- Returns the name, such as `minecraft:stone`. Read while `OCDAV` is set to read fully.

`6 getBlockId`
Looks a block up by name.
- Takes the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.
- Returns two bytes, the block id.
