# Documentation

Player documentation is the in-game manual, also available [online](https://fnuecke.github.io/oc2/manual/). The
documents here are primarily intended as reference for mod, data pack and guest software authoring.

| Document                   | Covers                                                                                 |
|----------------------------|----------------------------------------------------------------------------------------|
| [api.md](api.md)           | The Java device API for mods. Example mods are in [`examples`](../examples).           |
| [datapack.md](datapack.md) | Disk and firmware images, guest file systems, CP/M files and item tag filters.         |
| [hlapi.md](hlapi.md)       | The high-level API wire protocol, for clients in other languages or operating systems. |
| [mlapi.md](mlapi.md)       | The mid-level API register page: access, calls and events.                             |
| [riscv.md](riscv.md)       | The RISC-V machine: boot, memory map, device tree, firmware and image requirements.    |
| [z80.md](z80.md)           | The Z80 machine: memory, ports, enumeration, boot and interrupts.                      |
| [floppy.md](floppy.md)     | The Z80 floppy controller.                                                             |
| [serial.md](serial.md)     | Serial port registers.                                                                 |

Method references of individual devices are part of the
manual, [generated](../common/src/generated/resources/assets/oc2/doc/en_us/device/index.md) from the code.
