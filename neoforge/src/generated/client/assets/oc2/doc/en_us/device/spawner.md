# Spawner

## High-level API
Device name: `spawner`

Provided by monster spawners connected to a [bus interface](../block/bus_interface.md).

### Methods

`getEntityType():string`
Gets what the spawner spawns next.
- Returns the name of the creature, such as `minecraft:zombie`, if any.

## Mid-level API
Device name: `SPAWNR`

### Methods

`1 getEntityType`
Gets what the spawner spawns next.
- Returns the name of the creature, such as `minecraft:zombie`, if any. Read while `OCDAV` is set to read fully.
