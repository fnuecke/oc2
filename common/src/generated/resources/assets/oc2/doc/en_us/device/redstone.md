# Redstone

## High-level API
Device name: `redstone`

Provided by the [redstone interface](../block/redstone_interface.md) block and the [redstone interface card](../item/redstone_interface_card.md).

### Sides
The side parameter in the following methods comes in two forms.

Relative sides turn with the block, or with the computer holding the card. Each face of the redstone interface block has an indicator for convenience; the primary face is the one with a single marking. When looking at the primary face:
- `front` is the face we are looking at.
- `back` is the face behind the block.
- `left` is the face to our left.
- `right` is the face to our right.

Absolute sides always mean the same direction in the world, no matter how the block is placed: `north`, `south`, `west` and `east`.

`up` and `down` are the top and bottom faces, and mean the same thing either way.

Sides may also be given as a number instead of a name. Numbers are relative: `0` is `down`, `1` is `up`, `2` is `back`, `3` is `front`, `4` is `left` and `5` is `right`.

### Events
The redstone interface block sends `redstoneChanged` when the received signal on a side changes, so a program can wait for it instead of polling. The card sends no events. For example:
`local e = r:waitEvent(nil, "redstoneChanged")`
`print(e.data.side, e.data.value)`
- `side` is the relative name of the side, as in the "Sides" section.
- `value` is the new signal strength.

The device's own output counts towards the received signal, so setting an output may send this event as well.

### Methods

`getRedstoneInput(side:string):number`
Gets the received redstone signal for the specified side. The device's own output on that side counts towards the received signal.
- `side`: the side, by name (`front`, `back`, `left`, `right`, `up`, `down`, `north`, `south`, `west`, `east`) or by relative index.
- Returns the current input signal strength.

`getRedstoneOutput(side:string):number`
Gets the emitted redstone signal for the specified side. This is the value last set via setRedstoneOutput().
- `side`: the side, by name (`front`, `back`, `left`, `right`, `up`, `down`, `north`, `south`, `west`, `east`) or by relative index.
- Returns the current output signal strength.

`setRedstoneOutput(side:string, value:number)`
Sets the emitted redstone signal for the specified side.
- `side`: the side, by name (`front`, `back`, `left`, `right`, `up`, `down`, `north`, `south`, `west`, `east`) or by relative index.
- `value`: the signal strength to set, in the range of [0, 15]. Values outside are clamped.

## Mid-level API
Device name: `REDSTN`

Sides are numbered as in the "Sides" section above, and levels are in [0, 15].

`REDSTN.Z80` on the CP/M boot disk is an example consumer of the API. The [mid-level API](../mlapi.md) entry explains how to build and run it.

### Methods

`1 getRedstoneInput`
Reads the level received on that side.
- Takes one byte, the side.
- Returns one byte, the level.

`2 getRedstoneOutput`
Reads the level currently being sent on that side.
- Takes one byte, the side.
- Returns one byte, the level.

`3 setRedstoneOutput`
Sets the level sent on that side.
- Takes two bytes, the side and the level.
