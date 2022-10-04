# 声卡
![听到的寂静更少](item:oc2:sound_card)

声卡可以从其庞大的栩栩如生的样本库中播放各种声音效果。由于内部条件的限制，顺序播放需要一个短暂的暂停。尝试在此时间窗口中播放其他声音效果将不起作用.

这是一个高级设备.其必须通过高级设备API控制. 标准的Linux发行版提供了这个API的库. 例如:

`local d = require("devices")`  
`local s = d:find("sound")`  
`s:playSound("entity.creeper.primed")`

## API
设备名称: `sound`

### 使用方法
`playSound(name:string[,volume,pitch])` 播放指定的声音效果.
- `name` 是你想播放的声音效果的名称.
- `volume` 是声音的音量,  `1` 是正常的音量. 可选参数, 默认值为 `1`.
- `pitch` 是声音的音高, `1` 是正常的音高. 可选参数, 默认值为 `1`.
- 如果指定的声音无效则报错.

`findSound(name:string):table` 返回与给定名称匹配的可用声音效果的列表。请注意，结果数量有限，因此过于通用的查询将导致结果不完整.
- `name` 要查询的名称.
- 返回与查询字符串匹配的声音效果名称的列表.