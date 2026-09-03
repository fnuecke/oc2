# Redstone Interface
![Everything Red](block:oc2:redstone_interface)

The redstone interface provides an omnidirectional bus to receive and emit redstone signals.

Use this to interact with primitive devices, such as doors and lamps, or other machinery offering a redstone based protocol.

This is a high level device. It must be controlled using the high level device API. The default Linux distribution offers Lua libraries for this API. For example:  
`local d = require("devices")`  
`local r = d:find("redstone")`  
`r:setRedstoneOutput("up", 15)`

## API
Device name: `redstone`

### Sides
The side parameter in the following methods comes in two flavors.

Relative sides turn with the block. Each face of the block has an indicator for convenience; the primary face is the one with a single marking. When looking at the primary face:
- `front` is the face we are looking at.
- `back` is the face behind the block.
- `left` is the face to our left.
- `right` is the face to our right.

Absolute sides always mean the same direction in the world, no matter how the block is placed: `north`, `south`, `west` and `east`.

`up` and `down` are the top and bottom faces, and mean the same thing either way.

Sides may also be given as a number instead of a name. Numbers are relative: `0` is `down`, `1` is `up`, `2` is `back`, `3` is `front`, `4` is `left` and `5` is `right`.

### Methods
`getRedstoneInput(side):number` gets the received redstone signal for the specified side.
- `side` is a string representing the side to get the input on. See the "Sides" section.
- Returns the number representing the current input signal strength.

`setRedstoneOutput(side, value:number)` sets the emitted redstone signal for the specified side.
- `side` is a string representing the side to set the signal on. See the "Sides" section.
- `value` is a number representing the signal strength to set in the range of [0, 15].

`getRedstoneOutput(side):number` gets the emitted redstone signal for the specified side.
- `side` is a string representing the side to get the output on. See the "Sides" section.
- Returns the number representing the current output signal strength.