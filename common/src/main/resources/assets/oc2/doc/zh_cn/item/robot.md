# 机器人
![热烈我们的新机器人](item:oc2:robot)

机器人本质上是移动的 [电脑](../block/computer.md)。 由于它们的非固定性质，存在一些与普通 [电脑](../block/computer.md) 不同的行为。 它们无法连接到 [总线接口](../block/bus_interface.md)。 它们支持模块设备，而不是卡设备。 模块设备是考虑到机器人移动性的专用设备。

机器人拥有固定大小的物品栏，并拥有最先进的能量存储。 只有机器人的普通物品栏才能自动填充和清空，例如通过漏斗等设备。机器人的组件物品栏必须手动配置。

在默认配置中，机器人无法与自己的物品栏交互。 使用 [物品栏操作模块](inventory_operations_module.md) 使机器人能够移动自己物品栏中的物品，以及在其他物品栏中插入和提取物品。

为机器人充电，建议使用 [充电器](../block/charger.md)。 机器人可以在充电器顶部来为自己充电。，你也可以手动把机器人放在充电器的物品栏中。

默认的 Linux 发行版提供了一个实用的 Lua 库，`robot`，可以轻松控制机器人。 底层 API 提供异步移动方法。 该库实现了同步替代方案，使顺序编程更加方便。

## API
设备名称：`robot`

这是一个高级 API 设备。 它可以在默认的 Linux 发行版中使用 Lua 进行控制。 例如：
`local d = require("devices")`  
`local r = d:find("robot")`  
`d:move("forward")`

### 方向
以下方法中的方向参数表示相对于机器人的方向。 有效值为：`forward`(前)、`backward`(后)、`upward`(上)、`downward`(下) 表示移动动作，`left`(左转) 和 `right`(右转) 表示旋转动作。 这些方向以机器人执行动作时的正面来看的。

为方便起见，可以使用这些值的缩写形式别名：`back`(后)、`up`(上)、`down`(下)。 为了极其简洁，也可以使用每个方向的首字母。

### 方法
这些方法在底层机器人设备上可用。 请注意，该库为所有这些提供了有用的封装器。 建议使用库而不是直接与设备交互。

`getEnergyStored():number` 返回当前存储在机器人电池中的能量。
- 返回存储的能量。

`getEnergyCapacity():number` 返回可以存储在机器人电池中的最大能量。
- 返回存储的最大能量。

`getSelectedSlot():number` 返回机器人当前选择的物品栏。 这被许多模块用作隐式输入。
- 返回机器人当前选择的物品栏索引。

`setSelectedSlot(slot:number):number` 设置机器人选择的物品栏。 这被许多模块用作隐式输入。
- `slot` 是要选择的物品栏索引。
- 返回新选择的物品栏的索引。 如果指定的值无效，这可能与 `slot` 不同。

`getStackInSlot(slot:number):table` 获取指定物品栏中物品的描述。
- `slot` 是获取项目描述的物品栏索引。

`move(direction):boolean` 尝试将指定方向的移动动作排入运动队列。
- `direction` 是移动的方向。
- 返回操作是否成功入队。

`turn(direction):boolean` 尝试将转向动作排入运动队列。
- `direction` 是转向的方向。
- 返回操作是否成功入队。

`getLastActionId():number`返回最后一个入队动作的不透明 id。 在成功的 `move()` 或 `turn()` 调用后调用此函数以获取与入队操作关联的 id。
- 返回最后一个入队动作的 id。

`getQueuedActionCount():number`返回当前在运动队列中等待处理的操作数。 当入队失败时，使用它等待操作完成。
- 返回当前未决操作的数量。

`getActionResult(actionId:number):string` 返回具有指定 id 的操作的结果。 动作 ID 可以从 `getLastActionId()` 获得。 只有有限数量的过去行动结果可用。
- 返回指定操作 id 的结果，如果不可用则返回任何内容。 如果可用，可能的值为：`INCOMPLETE`(未完成)、`SUCCESS`(成功) 和 `FAILURE`(失败)。

### API 库
- 库名称：`robot`

这是一个 Lua 库。 它可以在默认的 Linux 发行版中使用。 例如：
`local r = require("robot")`  
`r.move("forward")`  
`r.turn("left")`

### 方法
`energy():number` 返回当前存储在机器人电池中的能量。
- 返回存储的能量。

`capacity():number` 返回可以存储在机器人电池中的最大能量。
- 返回存储的最大能量。

`slot():number`` 返回机器人当前选择的物品栏。 这被许多模块用作隐式输入。
- 返回机器人当前选择的物品栏索引。

`slot(slot:number):number` 设置机器人选择的物品栏。 这被许多模块用作隐式输入。
- `slot` 是要选择的物品栏索引。
- 返回新选择的物品栏的索引。 如果指定的值无效，这可能与 `slot` 不同。

`stack([slot:number]):table` 获取指定物品栏中物品的描述。
- `slot` 是获取项目描述的物品栏索引。

`move(direction):boolean` 尝试向指定的方向移动。 等待直到移动操作完成。
- `direction` 是移动的方向。
- 返回操作是否成功。

`moveAsync(direction)` 尝试异步移动到指定的方向。 等待塞直到操作成功入队。
- `direction` 是移动的方向。

`turn(direction):boolean` 试图转向指定的方向。  等待直到旋转操作完成。
- `direction` 是转向的方向。
- 返回操作是否成功。

`turnAsync(direction)` 尝试异步转向指定方向。 等待直到操作成功入队。
- `direction` 是转向的方向。