# Brewing Stand

## High-level API
Device name: `brewing_stand`

Provided by brewing stands connected to a [bus interface](../block/bus_interface.md). The stand's slots are available through the [item handler](item_handler.md) device.

### Methods

`getBrewTime():number`
Gets how much longer the current brew takes, in ticks.
- Returns the remaining time. `0` when not brewing.

`getFuel():number`
Gets how many more brews the stand's fuel lasts for.
- Returns the remaining brews.

## Mid-level API
Device name: `BREW`

### Methods

`1 getBrewTime`
Gets how much longer the current brew takes, in ticks.
- Returns two bytes, the time, low byte first. `0` when not brewing.

`2 getFuel`
Gets how many more brews the stand's fuel lasts for.
- Returns one byte, the remaining brews.
