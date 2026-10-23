# Beehive

## High-level API
Device name: `beehive`

Provided by beehives and bee nests connected to a [bus interface](../block/bus_interface.md).

### Methods

`getBeeCount():number`
Gets how many bees are inside the hive.
- Returns the number of bees.

`getHoneyLevel():number`
Gets how much honey the hive holds.
- Returns the honey level, from `0` to `5`.

`isSedated():boolean`
Gets whether bees are currently sedated.
- Returns `true` while the bees are calm.

## Mid-level API
Device name: `BHIVE`

### Methods

`1 getHoneyLevel`
Gets how much honey the hive holds.
- Returns one byte, the honey level, from `0` to `5`.

`2 getBeeCount`
Gets how many bees are inside the hive.
- Returns one byte, the number of bees.

`3 isSedated`
Gets whether bees are currently sedated.
- Returns one byte, `1` while bees are sedated, `0` otherwise.
