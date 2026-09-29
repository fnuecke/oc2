# Beacon

## High-level API
Device name: `beacon`

Provided by beacons connected to a [bus interface](../block/bus_interface.md).

### Methods

`getLevels():number`
Gets how many levels the pyramid below the beacon has.
- Returns the levels, from `0` to `4`.

`getPrimaryEffect():string`
Gets the primary effect the beacon grants.
- Returns the name of the effect, such as `minecraft:speed`, if set.

`getSecondaryEffect():string`
Gets the secondary effect the beacon grants.
- Returns the name of the effect, such as `minecraft:regeneration`, if set.

## Mid-level API
Device name: `BEACON`

### Methods

`1 getLevels`
Reads how many levels the pyramid below the beacon has.
- Returns one byte, the levels.

`2 getPrimaryEffect`
Reads the primary effect the beacon grants.
- Returns the name, such as `minecraft:speed`, if set. Read while `OCDAV` is set to read fully.

`3 getSecondaryEffect`
Reads the secondary effect the beacon grants.
- Returns the name, such as `minecraft:speed`, if set. Read while `OCDAV` is set to read fully.
