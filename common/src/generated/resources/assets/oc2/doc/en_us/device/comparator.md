# Comparator

## High-level API
Device name: `comparator`

Provided by redstone comparators connected to a [bus interface](../block/bus_interface.md).

### Methods

`getMode():string`
Gets the mode the comparator is in.
- Returns `compare` or `subtract`.

`getOutputSignal():number`
Gets the signal strength the comparator outputs.
- Returns the signal strength, from `0` to `15`.

`setMode(mode:string)`
Sets the mode the comparator is in.
- `mode`: `compare` or `subtract`.

## Mid-level API
Device name: `CMPRTR`

### Methods

`1 getOutputSignal`
Gets the signal strength the comparator outputs.
- Returns one byte, the signal strength, from `0` to `15`.

`2 getMode`
Gets the mode the comparator is in.
- Returns one byte, `0` is `compare`, `1` is `subtract`.

`3 setMode`
Sets the mode the comparator is in.
- Takes one byte, `0` is `compare`, `1` is `subtract`.
