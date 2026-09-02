# Z80 Processor
![Part think.](item:oc2:cpu_z80)

The processor is what a [computer](../block/computer.md) or [robot](robot.md) actually computes with. Without one in its processor slot, a machine will not start.

This is an 8-bit Z80 processor. Its [flash memory](flash_memory.md) holds a loader that boots CP/M 2.2 off a [floppy](floppy.md), it addresses 64K of memory whatever size module you install, and it wants very little hardware around it. Devices reachable through the [MLAPI](../mlapi.md), such as the [redstone interface](../block/redstone_interface.md), work as expected. Hard drives, network cards and the other native devices go unused, as the Z80 has no drivers for them. They can sit on the bus regardless.

Also see the [RISC-V processor](cpu_riscv.md).

## Starting Up
Build the machine as described in [getting started](../getting_started.md), using these parts:
- 1x [computer](../block/computer.md)
- 1x Z80 processor
- 1x **CP/M** [flash memory](flash_memory.md)
- 1x [disk drive](../block/disk_drive.md) on the bus, holding a **CP/M** [floppy](floppy.md)
- 1x [memory](memory.md), any size

Power it and turn it on. The loader finds the system floppy and boots CP/M to its prompt, `A>`. Run `DIR` to see what came on the disk:
- `DEVS.COM` prints the devices the machine can see
- `DEVLIB.INC` and `OCAPI.INC` are libraries for your own programs
- `REDSTN.Z80` is an onboarding example
- `ZMAC.COM` and `ZML.COM` are the assembler and the linker

Drive `A:` is the floppy you booted from, and you can write to it. Add another [disk drive](../block/disk_drive.md) with a blank [floppy](floppy.md) for more room; it shows up as `B:`.

A [robot](robot.md) has a floppy slot of its own, so it can carry its system disk without a disk drive.

You can now add more devices, depending on what you want to use your computer for. For information on how to control devices, and how to build and run the worked example, have a look at the [MLAPI](../mlapi.md) manual entry.
