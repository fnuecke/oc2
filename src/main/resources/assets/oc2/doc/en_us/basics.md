# Basics
This document contains some foundational information on how [computers](block/computer.md) work. This does not necessarily mean that this information is easy reading. Basic here means the general concepts in use.

## Architecture
Computers run on a single-core, general purpose RISC-V architecture. This means they have 64-bit registers and support floating point operations. This makes it possible to boot a modern Linux kernel on them.

### Native Devices
Native devices connected to computers are memory-mapped devices. This means they are mapped to some area in physical memory. Computers use regular Linux drivers to interact with them.

What devices are available, and where those devices live, is communicated to the software running on the computer using a flattened device tree. This data structure may contain additional information on the system in general. In particular, it also stores the size of the installed [memory](item/memory.md). Since this data structure gets copied during boot, it cannot be updated later on. This is the reason computers must be rebooted when changes to native devices, such as the [network interface card](item/network_interface_card.md), are made. Devices for which this is the case will typically have a corresponding note in their tooltip.

### HLAPI Devices
The other type of device are high level API devices, sometimes also called RPC devices. These devices use a single controller which communicates with the computers via single serial device. This controller device is present in all computers, and takes care of collecting messages from multiple devices, and dispatching messages to devices. The protocol this controller uses is a simple JSON message based protocol. The `devices` Lua library provided with the default Linux library wraps around the serial device used to connect to this controller. As such, using the `devices` library whenever using a HLAPI device, such as the [redstone interface block](block/redstone_interface.md), is *strongly* recommended.

Due to the nature of the employed protocol, data rate is rather limited. Most devices therefore usually only provide comparatively simple APIs which do not require sending large amounts of data either way.

## Configuration
Computers can be configured to some degree. The amount of memory, extra storage in the form of [hard drives](item/hard_drive.md) and most importantly, which cards to install, are largely up to the user. Note that the default Linux distribution does require at least 20M of memory, 24M are recommended.

Most components contribute to the overall energy consumption of a computer. To conserve energy, choosing only the necessary components is essential.

## Linux
The default Linux distribution contains some basic command line tools, and the ability to write and run Lua programs. For an overview of how to interact with HLAPI devices using Lua, please see the [scripting](scripting.md) manual entry.

Native devices use regular Linux drivers. For example, hard drives show up as `/dev/vdaX` devices and can be formatted and mounted regularly.

Computers provide two hardware clock (RTC) devices. The first one counts time in a scale most users will think in. It is used by default, for example by command line tools like `date` and `time`. The second one measures time as it works in the world the computer runs in. To obtain the current world time, use `hwclock -f /dev/rtc1`.