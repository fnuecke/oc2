# 构建互联网
要设置 [电脑](block/computer.md) 的网络，请使用 [网络连接口](block/network_connector.md) 和 [网线](item/network_cable.md) 连接起互联网。 最后，确保在每台 [电脑](block/computer.md) 都安装了 [网卡](item/network_interface_card.md)。

[网络连接口](block/network_connector.md) 可以连接到除 [电脑](block/computer.md) 正面以外的其他表面。 

对于更复杂的网络构建，可能需要 [网线集线器](block/network_hub.md)，它允许相互连接多个 [网络连接口](block/network_connector.md)。

## 跳数
网络数据包只能通过固定的最大“跃点”。一跳一般可以理解为一次转发操作。 例如，转发数据包的 [网络连接口](block/network_connector.md) 使用一跳。 将数据包转发到数据包未从其到达的每个连接器的 [网线集线器](block/network_hub.md) 使用一跳。 一旦剩余的跳数达到零，就不再转发数据包（即丢包）。 这避免了数据包在有循环的网络中永远循环。 也就是说，这是一种保障机制。 任何网络都不应包含循环，因为这也会导致同一数据包多次到达接收计算机。

## 设置电脑
要给 [电脑](block/computer.md) 联网，首先要安装 [网卡](item/network_interface_card.md) ，之后，使用默认的 Linux 发行版，运行命令 `setup-network.lua`。 这将提供一个向导来配置 [电脑](block/computer.md) 应如何连接到网络。 如果你知道自己在做什么，也可以用常规的方法设置 [电脑](block/computer.md)。

## DHCP
DHCP(动态主机配置协议) 是一种允许简化网络设置的协议。 只有一台 [电脑](block/computer.md) 需要具有静态配置的网络地址，网络中的所有其他 [电脑](block/computer.md) 可能会自动为其分配地址。 进行此设置时，请确保只有一台 [电脑](block/computer.md) 充当 DHCP 服务器。 还要确保没有 [电脑](block/computer.md) 使用属于动态分布的 IP 地址范围的静态 IP 地址。

## 指令
如果你成功构建了互联网，让多个 [电脑](block/computer.md) 互相连接，你就可以远程操控 [电脑](block/computer.md)。如使用 `scp` 在 [电脑](block/computer.md) 之间复制文件，使用 `ssh` 登录到远程 [电脑](block/computer.md) ，并使用 `socket` 库在 Lua 中编写自定义网络程序。关于如何使用 `socket` 库，请参考 [socket官方示例（github）](https://github.com/diegonehab/luasocket/tree/master/samples)。