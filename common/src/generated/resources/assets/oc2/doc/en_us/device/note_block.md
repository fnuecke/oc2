# Note Block

## High-level API
Device name: `note_block`

Provided by note blocks connected to a [bus interface](../block/bus_interface.md).

The device changes what the note block is set to. It does not play it, use redstone signals for that.

Instrument names include: `harp` or `bass`, `bell`, `chime`, `flute`, `guitar`, `pling`, `xylophone`, `bit`.

Note that the instrument configuration is transient. Environmental changes will override it back to its natural configuration.

### Methods

`getInstrument():string`
Gets the name of the instrument the block plays.
- Returns the instrument name.

`getNote():number`
Gets the note the block is tuned to.
- Returns a note from `0` to `24`.

`setInstrument(instrument:string)`
Sets the instrument the block plays.
- `instrument`: the instrument name.

`setNote(note:number)`
Tunes the block to a note.
- `note`: the note to tune to, from `0` to `24`.

## Mid-level API
Device name: `NOTE`

### Methods

`1 getNote`
Gets the note the block is tuned to.
- Returns one byte, the note, from `0` to `24`.

`2 setNote`
Tunes the block to a note.
- Takes one byte, the note, from `0` to `24`.

`3 getInstrument`
Gets the name of the instrument the block plays.
- Returns the instrument name. Read while `OCDAV` is set to read fully.

`4 setInstrument`
Sets the instrument the block plays.
- Takes the instrument name.
