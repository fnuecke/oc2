# Charger

## High-level API
Device name: `charger`

Provided by the [charger](../block/charger.md) block.

### Methods

`isCharging():boolean`
Gets whether the charger is currently transferring energy to something on top of it.
- Returns whether energy is being transferred.

## Mid-level API
Device name: `CHARGR`

### Methods

`1 isCharging`
Gets whether the charger is currently transferring energy to something on top of it.
- Returns one byte, `1` if energy is being transferred, `0` otherwise.
