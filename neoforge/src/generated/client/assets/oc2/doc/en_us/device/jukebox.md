# Jukebox

## High-level API
Device name: `jukebox`

Provided by jukeboxes connected to a [bus interface](../block/bus_interface.md). Times are in ticks.

### Methods

`getElapsed():number`
Gets how long the jukebox has been playing the current song.
- Returns the time since the song started.

`getSong():string`
Gets the song the jukebox is playing.
- Returns the name of the song, such as `minecraft:cat`, if any.

`getSongLength():number`
Gets how long the song the jukebox is playing lasts.
- Returns the length of the song.

`isPlaying():boolean`
Gets whether the jukebox is playing a song.
- Returns whether a track is playing.

## Mid-level API
Device name: `JUKEBX`

Times are two bytes, low byte first, capped at 65535.

### Methods

`1 isPlaying`
Gets whether the jukebox is playing a song.
- Returns one byte, `1` while a track is playing, `0` otherwise.

`2 getSong`
Gets the song the jukebox is playing.
- Returns the name of the song, such as `minecraft:cat`, if any. Read while `OCDAV` is set to read fully.

`3 getSongLength`
Reads how long the song the jukebox is playing lasts.
- Returns two bytes, the length of the song.

`4 getElapsed`
Reads how long the jukebox has been playing the current song.
- Returns two bytes, the time since the song started.
