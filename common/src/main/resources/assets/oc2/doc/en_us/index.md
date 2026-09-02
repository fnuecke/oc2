# The Computerists Handbook
Hello, greetings, welcome! If you've come here, you either stumbled in by accident, are interested in [building your first computer](getting_started.md), or maybe you are looking for information on a particular [block](block/index.md) or [item](item/index.md)?

## Overview
[Computers](block/computer.md) offer a variety of uses, from recreational to large-scale, highly customizable automation of other machines and devices.

At the heart of each computer lies its CPU and accompanying operating system (OS). The OS provided for the [RISC-V architecture](item/cpu_riscv.md) is a very basic Linux distribution. It offers just the most basic command line tools, as well as the means to write and run Lua programs.

Lua is very relevant when it comes to interacting with high-level API ([HLAPI](hlapi.md)) devices, such as the [redstone interface block](block/redstone_interface.md). With the OS come some utility libraries to enable interacting with such devices, in particular the `devices` library.

On the other end of the scale are native devices, such as [hard drives](item/hard_drive.md) and the [network interface card](item/network_interface_card.md). These devices are controlled by native Linux drivers and will require a restart of the system when added or removed. Most devices you will encounter will be HLAPI devices, however.

For the more adventurous, there is the [Z80 architecture](item/cpu_z80.md), which offers a much cheaper, but also much more limited environment with CP/M as its OS. It interacts with mid-level ([MLAPI](mlapi.md)) devices, such as the [redstone interface block](block/redstone_interface.md). With this OS as well come some utilities, in particular the `DEVLIB.INC` and `OCLIB.INC` libraries.

## Getting Started
If you just want to get running quickly, read the [getting started guide](getting_started.md). It contains a step-by-step description on how to build your first computer and how to start working with it. To learn more about some topic in particular, see the referenced topical pages in the "Reference" section. 

## Reference
This manual contains reference information on the blocks and items related to computers and the like.

If you're looking for information on a particular block or item, have a look at the respective glossaries:
- [List of blocks](block/index.md)
- [List of items](item/index.md)

If you're interested in a particular topic, there are some overview entries for the more common ones:
- [Basics](basics.md)
- [High-level API](hlapi.md)
- [Mid-level API](mlapi.md)
- [Robotics](robotics.md)
- [Networking](networking.md)
