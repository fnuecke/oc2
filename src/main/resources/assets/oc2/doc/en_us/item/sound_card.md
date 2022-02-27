# Sound Card
![Less sound of silence](item:oc2:sound_card)

The sound card enables playing back various sound effects from its vast library of life-like samples. Due to internal engineering constraints, playback of sequential effects requires a small pause. Attempts to play back additional effects in this time window will have no effect.

This is a high level device. It must be controlled using the high level device API. The default Linux distribution offers Lua libraries for this API. For example:  
`local d = require("devices")`  
`local s = d:find("sound")`  
`s:playSound("entity.creeper.primed")`

## API
Device name: `sound`

### Methods
`playSound(name:string[,volume,pitch])` plays back the sound effect with the specified name.
- `name` is the name of the effect to play.
- `volume` is the volume at which to play the effect, with `1` being the normal volume. Optional, defaulting to `1`.
- `pitch` is the volume at which to play the effect, with `1` being the normal pitch. Optional, defaulting to `1`.
- Throws if the specified name is invalid.

`findSound(name:string):table` returns a list of available sound effects matching the given name. Note that the number of results is limited, so overly generic queries will result in truncated results.
- `name` the name query to search for.
- Returns a list of sound effect names matching the query string.