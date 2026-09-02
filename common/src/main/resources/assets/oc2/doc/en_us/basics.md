# Basics
This document contains some foundational information on how [computers](block/computer.md) work. This does not necessarily mean that this information is easy reading. Basic here means the general concepts in use.

## Architecture
Computers need a CPU to run. Options are the [Z80](item/cpu_z80.md) and the [RISC-V](item/cpu_riscv.md) CPUs. See their respective documentation for details. The Z80 can run CP/M and interact with devices using the [mid-level API](mlapi.md) (MLAPI), RISC-V can run Linux and interact with devices using the [high-level API](hlapi.md) (HLAPI).

### Native Devices
Native devices connected to computers are memory-mapped devices on RISC-V and port-mapped devices on Z80. This means they are mapped to some area in physical memory or a dedicated device port. Computers use regular Linux drivers or the shipped BIOS to interact with them. Z80 is limited to disk drives.

What devices are available, and where those devices live, is communicated to the software running on the computer using a flattened device tree on the RISC-V, an enumeration window on the Z80. This device tree may contain additional information on the system in general. In particular, it also stores the size of the installed [memory](item/memory.md). Since this data structure gets copied during boot, it cannot be updated later on. This is the reason computers must be rebooted when changes to native devices, such as the [network interface card](item/network_interface_card.md), are made. Devices for which this is the case will typically have a corresponding note in their tooltip.

### HLAPI and MLAPI Devices
The other type of device are high- and mid-level API devices, sometimes also called RPC or IO devices. These devices use a single controller which communicates with the computers via single serial device. This controller device is present in all computers, and takes care of collecting messages from multiple devices, and dispatching messages to devices. The protocol this controller uses is a simple JSON message based or register-based binary protocol, respectively. The `devices` Lua library provided with the default Linux library wraps around the serial device used to connect to this controller. The `DEVLIB.INC` and `OCLIB.INC` libraries shipping with the default CP/M image eases access to the enumeration window. As such, using these libraries whenever using a HLAPI/MLAPI device, such as the [redstone interface block](block/redstone_interface.md), is *strongly* recommended.

Due to the nature of the employed protocols, data rate is rather limited. Most devices therefore usually only provide comparatively simple APIs which do not require sending large amounts of data either way.

## Configuration
Computers can be configured to some degree. The amount of memory, extra storage in the form of [hard drives](item/hard_drive.md) and most importantly, which cards to install, are largely up to the user. Note that the default Linux distribution does require at least 10M of memory, 12M are recommended. CP/M is less hungry and any modern RAM will satisfy it's 64KB need.

Most components contribute to the overall energy consumption of a computer. To conserve energy, choosing only the necessary components is essential.

## Linux
The default Linux distribution contains some basic command line tools, and the ability to write and run Lua programs. For an overview of how to interact with [HLAPI](hlapi.md) devices using Lua, please see that manual entry.

Native devices use regular Linux drivers. For example, hard drives show up as `/dev/vdaX` devices and can be formatted and mounted regularly.

Computers provide two hardware clock (RTC) devices. The first one counts time in a scale most users will think in. It is used by default, for example by command line tools like `date` and `time`. The second one measures time as it works in the world the computer runs in. To obtain the current world time, use `hwclock -f /dev/rtc1`.

## CP/M
The default CP/M distribution contains some basic command line tools, and the ability to write, assemble, link and run an assembly program. For an overview on how to interact with [MLAPI](mlapi.md) devices using assembly, please see that manual entry.

Disk drives use the regular BIOS. For example, extra floppy drives show up as lettered drives such as `B:`.
