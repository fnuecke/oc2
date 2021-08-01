# The Computerists Handbook
Hello, greetings, welcome! If you've come here, you either stumbled in by accident, are interested in [building your first computer](getting_started.md), or maybe you are looking for information on a particular [block](block/index.md) or [item](item/index.md)?

## Overview
[Computers](block/computer.md) offer a variety of uses, from recreational to large-scale, highly customizable automation of other machines and devices.

At the heart of each computer lies its operating system (OS). The default OS provided is a very basic Linux distribution. It offers just the most basic command line tools, as well as the means to write and run Lua programs.

Lua is very relevant when it comes to interacting with high level API devices (HLAPI), such as the [redstone interface block](block/redstone_interface.md). With the default OS come some utility libraries to enable interacting with such devices, in particular the `devices` library. To learn more about how to interact with HLAPI devices from Lua, see the [scripting](scripting.md) manual entry.

On the other end of the scale are native devices, such as [hard drives](item/hard_drive.md) and the [network interface card](item/network_interface_card.md). These devices are controlled by native Linux drivers and will require a restart of the system when added or removed. Most devices you will encounter will be HLAPI devices, however.

## Getting Started
If you just want to get running quickly, read the [getting started guide](getting_started.md). It contains a step-by-step description on how to build your first computer and how to start working with it. To learn more about some topic in particular, see the referenced topical pages in the "Reference" section. 

## Reference
This manual contains reference information on the blocks and items related to computers and the like.

If you're looking for information on a particular block or item, have a look at the respective glossaries:
- [List of blocks](block/index.md)
- [List of items](item/index.md)

If you're interested in a particular topic, there are some overview entries for the more common ones:
- [Basics](basics.md)
- [Scripting](scripting.md)
- [Robotics](robotics.md)
- [Networking](networking.md)