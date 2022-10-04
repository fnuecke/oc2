# 脚本
使用Lua脚本控制设备是使用 [电脑](block/computer.md) 时的核心玩法。许多设备是所谓的高级 API (HLAPI) 设备，而它们并不是使用常规的Linux驱动器来控制的，而是通过一个简单的 RPC 系统，在串行设备上使用 JSON 通信。

## Devices 库
默认的 Linux 发行版包括更易于访问 HLAPI 设备的库。 `devices` 库提供了用于发现设备并在其上调用方法的实用程序，以及在可用时获取有关设备的文档。

要使用 `devices` 库，请使用 `require("devices")` 导入它。

### 方法
`list():table` 返回当前所有可用设备的列表。返回表中的每个条目代表一个设备，包括其类型名称和唯一标识符。
- 返回连接的 HLAPI 设备列表。

`get(id:string):Device` 返回具有指定标识符的设备的封装。 这类似于 `find`，但采用特定标识符而不是类型名称。
- `id` 是设备的唯一标识符。
- 返回指定设备的包装器。

`find(typeName):Device` 返回指定类型设备的封装。 如果有很多这种类型的设备，则无法确定返回哪一个。 也可以使用 [总线接口](block/bus_interface.md) 中设置的别名。
- `typeName` 是要为其查找设备的设备类型。
- 返回指定设备的设备的封装。

`methods(id:string):table` 返回具有指定标识符的设备提供的方法列表。 要以更易读的方式获取方法列表，请使用设备封装的方法并将其转换为字符串。 另请参阅有关设备封装器类型的部分。
- `id` 是设备的唯一标识符。
- 返回设备提供的方法列表。
- 如果获取方法列表失败，则抛出错误。

`invoke(id:string, methodName:string, ...):any` 在具有指定标识符的设备上调用具有指定名称的方法，传递其他额外参数。不建议直接使用此方法，更推荐使用设备封装的方法。另请参阅有关设备封装器类型的部分。
-“id”是设备的唯一标识符。
-'methodName'是要调用的方法的名称。
- `...` 是要传递给方法的参数。
-返回方法的结果。
-如果调用失败或方法引发异常，则抛出错误。

## 设备封装器
从 `device` 库返回的设备是 `Device` 类型的对象。 这是一个存储设备标识符的封装器。

其主要目的是支持无缝调用此设备公开的方法，以及访问文档（如果可用）的便捷方式。

要在此类包装器上调用方法，请使用冒号表示法和方法的名称。 例如：
`wrapper:someMethod(1, 2, 3)`

获取设备的文档，将要对其进行字符串化： 在Lua脚本中，使用 `=wrapper`。

## 举例
在这个例子中，我们将控制一个 [红石接口](block/redstone_interface.md)。 首先，放置模块并使用 [总线电缆](block/bus_cable.md) 和 [总线接口](block/bus_interface.md) 将其连接到计算机。

![连接红石接口](../img/scripting_redstone_interface.png)

我们在 [红石接口](block/redstone_interface.md) 顶部上放个红石灯，来测试一下。

先检查设备和 [电脑](block/computer.md) 之间的连接，在命令行运行命令`lsdev.lua`。 这列出了所有连接的 HLAPI 设备的标识符和类型名称。 其中应该包含有`redstone`。

在命令行运行 `lua` 以交互模式启动 Lua。

![交互模式下的 Lua](../img/scripting_lua_interactive.png)

导入 `Device` 库并将其存储在名为 `d` 的变量中：
`d = require("devices")`

然后获取 [红石接口](block/redstone_interface.md) 的`Device`封装器并将其存储在名为 `r` 的变量中：
`r = d:find("redstone")`

这个调用中的 `redstone` 是设备类型名称，这与我们之前使用 `lsdev.lua` 检查时的名称相同。
我们现在有一个 [红石接口](block/redstone_interface.md) 的封装器，我们可以在其上调用方法。 要获取可用方法的列表，请输入 `=r`。

这次例子中，我们需要使用 `setRedstoneOutput` 方法。 它是用来设置从 [红石接口](block/redstone_interface.md) 发出的红石信号。

为了点亮我们的红石灯，我们得从 [红石接口](block/redstone_interface.md) 的顶面发射一个红石信号：
`r:setRedstoneOutput("up", 15)`

红石灯现在应该亮了！

![成功点亮红石灯](../img/scripting_lamp.png)

有了这个，您就有了工具来了解连接设备的名称、它们提供的方法以及如何获取它们的文档。试试使用 [红石接口](block/redstone_interface.md) 的其他方法来读取传入的红石信号，或者尝试用其他设备！
