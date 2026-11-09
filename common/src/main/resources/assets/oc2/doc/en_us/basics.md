# Basics
This entry describes the general concepts behind [computers](block/computer.md): which processors and operating systems there are, and how software talks to devices. For step-by-step instructions, see the [getting started guide](systems/index.md).

## Architectures
Computers need a processor (CPU) to run. There are two architectures:
- [RISC-V](item/cpu_riscv.md), a 64-bit processor. It runs OpenOS II and Linux.
- [Z80](item/cpu_z80.md), an 8-bit processor. It runs CP/M. It is cheaper, but much more limited.

## Operating Systems
Three operating systems (OS) are supplied:
- [OpenOS II](systems/openos2.md), a small OS with a shell and programs written in Lua.
- [Linux](systems/linux.md), a small Linux distribution with common command line tools, Lua, MicroPython and a C compiler.
- [CP/M](systems/cpm.md), CP/M 2.2 with an assembler, a linker and a line editor.

## Native Devices
Native devices are memory-mapped devices on RISC-V and port-mapped devices on Z80. This means they are mapped to some area in physical memory or to a dedicated device port. The OS uses its own drivers to talk to them. Examples are [hard drives](item/hard_drive.md), the [network interface card](item/network_interface_card.md) and the [sound card](item/sound_card.md).

Native devices may only be used by one computer at a time, so block devices only have one face that connects to the bus, usually their back or bottom side.

Which devices are available, and where those devices live, is communicated to the software running on the computer using a flattened device tree on RISC-V, and an enumeration window on Z80. The device tree also stores the size of the installed [memory](item/memory.md). Since this data structure gets copied during boot, it cannot be updated later on. This is the reason computers must be rebooted when changes to native devices are made. Devices for which this is the case will typically have a corresponding note in their tooltip.

Native devices are supported by:
- OpenOS II, all native devices except the network interface card and network tunnel devices.
- Linux, all native devices.
- CP/M, disk drives and serial interface cards.

## HLAPI and MLAPI Devices
Most devices are high-level API ([HLAPI](hlapi.md)) or mid-level API ([MLAPI](mlapi.md)) devices, such as the [redstone interface](block/redstone_interface.md). They all share a single controller, which is present in every computer. The controller collects messages from the devices and dispatches messages to them. Unlike native devices, these devices can be added and removed while the computer is running.

The HLAPI uses JSON messages over a serial device. Programs call device methods by name, and the `devices` Lua library takes care of the protocol. It is supported by:
- OpenOS II
- Linux

The MLAPI uses a register-based binary protocol, on an I/O port on the Z80 and in a memory page on RISC-V. Programs pick a function by number, and its arguments and results are plain bytes. Most HLAPI devices also offer the MLAPI. It is supported by:
- CP/M, using the `OCAPI.INC` library
- Linux, using the `mlapi.h` C header

Due to the nature of the employed protocols, data rate is rather limited. Most devices therefore only provide comparatively simple APIs which do not require sending large amounts of data either way.

## Energy
Most components contribute to the overall energy consumption of a computer. To conserve energy, being deliberate bout the components used is essential.
