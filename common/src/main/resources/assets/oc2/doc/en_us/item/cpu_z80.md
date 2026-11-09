# Z80 Processor
![Part think.](item:oc2:cpu_z80)

The processor is what a [computer](../block/computer.md) or [robot](robot.md) actually computes with. Without one in its processor slot, a machine will not start.

This is an 8-bit Z80 processor. It runs [CP/M](../systems/cpm.md). It addresses 64K of memory, whatever size module you install, and it needs very little hardware around it. Devices available through the [MLAPI](../mlapi.md), such as the [redstone interface](../block/redstone_interface.md), work as expected. Hard drives, network cards and the other native devices go unused, as the Z80 has no drivers for them. They can stay on the bus regardless.

Crafting it with blank flash memory writes the CP/M loader. Crafting it with a blank floppy writes the CP/M system.

Also see the [RISC-V processor](cpu_riscv.md).
