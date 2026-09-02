# Z80 Processor
![Part think.](item:oc2:cpu_z80)

The processor is what a [computer](../block/computer.md) or [robot](robot.md) actually computes with. Without one in its processor slot, a machine will not start.

This is the 8-bit Z80 processor. It boots CP/M 2.2 from a [flash memory](flash_memory.md) chip, addresses 64K of memory whatever size module you install, and wants very little hardware around it. Devices reachable through the [MLAPI](../mlapi.md), such as the [redstone interface](../block/redstone_interface.md), work as expected. Hard drives, network cards and the other native devices go unused, as the Z80 has no drivers for them. They can sit on the bus regardless.

Also see the [RISC-V processor](cpu_riscv.md).

## Starting Up
Build the machine as described in [getting started](../getting_started.md), using these parts:
- 1x [computer](../block/computer.md)
- 1x Z80 processor
- 1x **CP/M** [flash memory](flash_memory.md)
- 1x [memory](memory.md), any size

Power it and turn it on. The machine boots to the CP/M prompt, `A>`. Run `DIR` to see what came with it:
- `DEVS.COM` prints the devices the machine can see
- `DEVLIB.INC` and `OCAPI.INC` are libraries for your own programs
- `REDSTN.Z80` is a worked example
- `ZMAC.COM` and `ZML.COM` are the assembler and the linker

Drive `A:` is read only. For anywhere to put your own files, add a [disk drive](../block/disk_drive.md) to the bus with a [floppy](floppy.md) in it. It shows up as `B:`.

You can now add more devices, depending on what you want to use your computer for. For information on how to control devices, and how to build and run the worked example, have a look at the [MLAPI](../mlapi.md) manual entry.
