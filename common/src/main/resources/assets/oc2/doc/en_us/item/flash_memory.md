# Flash Memory
![Not that Flash](item:oc2:flash_memory)

Flash memory contains the initial code loaded into a [computer's](../block/computer.md) memory upon boot. This typically at least includes the firmware for the system.

Blank flash memory holds no firmware. To write firmware to it, craft it together with the processor that firmware is built for: a [RISC-V processor](cpu_riscv.md) writes the Linux firmware, a [Z80 processor](cpu_z80.md) writes the CP/M firmware.

Crafting programmed flash memory with a [wrench](wrench.md) erases it back to blank flash memory, discarding the firmware and anything stored on it.
