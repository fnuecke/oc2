# 物品栏操作模块
![你的就是我的](item:oc2:inventory_operations_module)

物品栏操作模块为 [机器人](robot.md) 提供了从世界物品栏中插入和提取物品的能力。 这支持方块和实体物品栏。

## API
设备名称： `inventory_operations`

这是一个高级 API 设备。 它可以在默认的 Linux 发行版中使用 Lua 进行控制。 例如：
`local d = require("devices")`  
`local m = d:find("inventory_operations")`  
`m:drop(1, "front")`

### 方向
以下方法中的 side 参数表示机器人视角的方向。 有效值为：`front`(正面)、`up`(上面) 和 `down`(下面)。

### 方法
`move(fromSlot:number,intoSlot:number,count:number)` 尝试将指定数量的物品从一个机器人物品栏移动到另一个。
- `fromSlot` 是从中提取物品的物品栏槽位。
- `intoSlot` 是插入物品的物品栏槽位。
- `count` 是要移动的物品数。

`drop(count:number[,side]):number` 尝试从指定物品栏向指定方向投掷物品。 如果没有物品栏，它会将物品放入物品栏或世界中。
- `count` 是要投掷的物品数。
- `side` 是投掷项目的相对方向。可选，默认为 `front`(正面)。 请参阅“方向”部分。
- 返回被投掷的物品数。

`dropInto(intoSlot:number,count:number[,side]):number` 尝试将物品从指定槽位按指定方向放入物品栏的指定槽位。 它只会将物品放入物品栏中。
- `intoSlot` 是插入物品的插槽。
- `count` 是要放入的物品数。
- `side` 是放置项目的相对方向。可选，默认为 `front`(正面)。 请参阅“方向”部分。
- 返回被放入的物品数。

`take(count:number[,side]):number` tries to take the specified number of items from the specified direction. It will take items from either an inventory, or the world if no inventory is present.
- `count` is the number of items to take.
- `side` is the relative direction to take the items from. Optional, defaults to `front`. See the "Sides" section.
- Returns the number of items taken.

`takeFrom(fromSlot:number,count:number[,side]):number` tries to take the specified number of items from the specified slot from an inventory in the specified direction. It will only take items from an inventory.
- `fromSlot` is the slot to take the items from.
- `count` is the number of items to take.
- `side` is the relative direction to take the items from. Optional, defaults to `front`. See the "Sides" section.
- Returns the number of items taken.