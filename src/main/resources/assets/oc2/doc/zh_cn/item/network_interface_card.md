# 网络接口卡
![与陌生人对话](item:oc2:network_interface_card)

网络接口卡 (NIC) 允许 [电脑](../block/computer.md) 接受和发送信息从一个与电脑相连的网络连接器 [网络连接器](../block/network_connector.md).

这些卡可以被设置为仅从特定的面进行连接(在手中设置). 这允许多张卡组成一个自定义的路由器，例如.

在使用一个标准的Linux发行版时, 这个设备将会被识别为一个标准的以太网设备. 网络可以有用户设置, 或使用一个快捷设置脚本`setup-network.lua`. 这个脚本也提供了修复IP地址的选项, 或将其设置为DHCP(动态IP分配). 在DHCP设置下, 实际上需要一个网络系统内的电脑承担DHCP服务器的任务.

在完成初始设置后, 使用 `ifconfig` 命令以查看当前使用到的IP.

Computers *have to be shut down* before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.
