# Furnace

## High-level API
Device name: `furnace`

Provided by furnaces, smokers and blast furnaces connected to a [bus interface](../block/bus_interface.md). Times are in ticks. Slots are available through the [item handler](item_handler.md) device.

### Methods

`getBurnDuration():number`
Gets how long the current fuel burns in total.
- Returns the burn time of the last consumed fuel.

`getBurnTime():number`
Gets how much longer the current fuel burns.
- Returns the remaining burn time.

`getCookTime():number`
Gets how long the current item has been cooking.
- Returns the cooking progress.

`getCookTimeTotal():number`
Gets how long the current item needs to cook.
- Returns the time the item needs.

`isBurning():boolean`
Gets whether the furnace is burning fuel.
- Returns whether fuel is currently burning.

## Mid-level API
Device name: `FRNACE`

Times are two bytes, low byte first, capped at 65535.

### Methods

`1 getBurnTime`
Gets how much longer the current fuel burns.
- Returns two bytes, the remaining burn time.

`2 getBurnDuration`
Gets how long the current fuel burns in total.
- Returns two bytes, the burn time of the last consumed fuel.

`3 getCookTime`
Gets how long the current item has been cooking.
- Returns two bytes, the cooking progress.

`4 getCookTimeTotal`
Gets how long the current item needs to cook.
- Returns two bytes, the time the item needs.

`5 isBurning`
Gets whether the furnace is burning fuel.
- Returns one byte, `1` while fuel is currently burning, `0` otherwise.
