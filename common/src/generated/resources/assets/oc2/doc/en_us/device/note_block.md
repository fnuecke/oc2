# Note Block

## High-level API
Device name: `note_block`

Provided by note blocks connected to a [bus interface](../block/bus_interface.md).

The device changes what the note block is set to. It does not play it, use redstone signals for that.

Note that the instrument configuration is transient. Environmental changes will override it back to its natural configuration.

### Methods

`getInstrument():string`
Returns the name of the instrument the block plays.
- Returns the instrument name, such as `harp` or `bit`.

`getNote():number`
Returns the note the block is tuned to.
- Returns a note from `0` to `24`.

`setInstrument(instrument:string)`
Changes the instrument the block plays.
- `instrument`: the name of the instrument, such as `harp`, `bass`, `bell`, `chime`, `flute`, `guitar`, `pling`, `xylophone` or `bit`.

`setNote(note:number)`
Tunes the block to a note.
- `note`: the note to tune to, from `0` to `24`.
