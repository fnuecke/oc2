# 红石接口（尚未完成翻译）
![所有东西都是红的！](block:oc2:redstone_interface)

（译者注：上文的Red（stone）略去了Stone，故做此翻译）

红石接口提供了发射和接收红石信号的全向总线.

用该设备来与红石设备互动，如门和灯, 或者其他提供红石通讯方式的设备.

这是一个高级设备.其必须通过高级设备API控制. 标准的Linux发行版提供了这个API的库. 例如:  
`local d = require("devices")`  
`local r = d:find("redstone")`  
`r:setRedstoneOutput("up", 15)`

## API
设备名称: `redstone`

### Sides
The side parameter in the following methods represents a side local to the device block. Valid values are: `up`, `down`, `left`, `right`, `front`, `back`, `north`, `south`, `west` and `east`.

Each face of the block has an indicator for convenience. Side names represent the names with the block seen from the primary face (indicated by a single marking). When looking at the primary face:
- `front` and `south` is the face we are looking at.
- `back` and `north` is the face behind the block.
- `left` and `west` is the face to our left.
- `right` and `east` is the face to our right.
- `up` and `down` are the top and bottom faces.

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