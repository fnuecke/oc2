# Composter

## High-level API
Device name: `composter`

Provided by composters connected to a [bus interface](../block/bus_interface.md).

### Methods

`getLevel():number`
Gets how full the composter is.
- Returns the level, from `0` to `8`.

## Mid-level API
Device name: `CMPSTR`

### Methods

`1 getLevel`
Gets how full the composter is.
- Returns one byte, the level, from `0` to `8`.
