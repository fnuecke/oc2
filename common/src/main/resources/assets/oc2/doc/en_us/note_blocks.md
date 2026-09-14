# Note Blocks
A computer can configure note blocks connected to a [bus interface](block/bus_interface.md).

## High-level API
Device name: `note_block`

The device changes what the note block is set to. It does not play it, use redstone signals for that.

For example, to tune a connected note block two semitones up:  
`local d = require("devices")`  
`local note_block = d:find("note_block")`  
`note_block:setNote(note_block:getNote() + 2)`

Note that the instrument configuration is transient. Environmental changes will override it back to its natural configuration.

### Methods
`getNote():number` returns the note the block is tuned to.
- Returns a note from `0` to `24`.

`setNote(note:number)` tunes the block to a note.
- `note` is the note to tune to, from `0` to `24`.

`getInstrument():string` returns the name of the instrument the block plays, such as `harp` or `bit`.

`setInstrument(instrument:string)` changes the instrument the block plays.
- `instrument` is the name of the instrument, such as `harp`, `bass`, `bell`, `chime`, `flute`, `guitar`, `pling`, `xylophone` or `bit`.
