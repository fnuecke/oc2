# CP/M
CP/M 2.2 runs on the [Z80 processor](../item/cpu_z80.md). It is a much cheaper, but also much more limited environment. Programs are written in Z80 assembly, and assembled and linked on the computer itself. It supports [disk drives](../block/disk_drive.md), [serial interface cards](../item/serial_interface_card.md) and [MLAPI](../mlapi.md) devices. Hard drives, network cards and the other native devices are ignored without effect.

## Parts
Build the computer as described in [getting started](index.md), using these parts:
- 1x [computer](../block/computer.md)
- 1x [Z80 processor](../item/cpu_z80.md)
- 1x **CP/M 2.2** [flash memory](../item/flash_memory.md), crafted from flash memory and a Z80 processor
- 1x [disk drive](../block/disk_drive.md) on the bus, holding a **CP/M 2.2** [floppy](../item/floppy.md), crafted from a floppy and a Z80 processor
- 1x [memory](../item/memory.md), any size

A [robot](../item/robot.md) has a floppy slot of its own, so it can have a system disk without a disk drive.

## Starting Up
Power the computer and turn it on. The loader in the flash memory finds the system floppy and boots CP/M to its prompt, `A>`. Run `DIR` to see what came on the disk:
- `DEVS.COM` prints the devices the computer can see.
- `DEVLIB.INC`, `OCAPI.INC` and `SERIAL.INC` are libraries for your own programs.
- `REDSTN.Z80`, `REDWAIT.Z80` and `SERCHAT.Z80` are example programs.
- `ZMAC.COM` and `ZML.COM` are the assembler and the linker.
- `ED.COM` is a line editor, `STAT.COM` shows free space and device assignments.
- `TERM.COM` is a terminal for the serial interface card.

## Drives
Drives are lettered in the order they are found, starting at `A:`. The floppy the computer booted from always comes first. Add another [disk drive](../block/disk_drive.md) with a blank [floppy](../item/floppy.md) for more room, it shows up as `B:`. Type `B:` to switch to it.

The prompt has a few built-in commands: `DIR` lists files, `TYPE` prints a text file, `ERA` deletes files and `REN NEW.TXT=OLD.TXT` renames a file.

## Building Programs
`ED B:PROG.Z80` creates and edits a source file. To assemble and link a program onto `B:`, while staying on `A:`:  
`ZMAC REDSTN /OB:REDSTN /E`  
`ZML B:REDSTN`  
`B:REDSTN 1 15`

The first command assembles the source, the second links it into `B:REDSTN.COM`. The [mid-level API](../mlapi.md) entry explains the options and what this example does.

## Devices
To control devices such as the [redstone interface](../block/redstone_interface.md), see the [mid-level API](../mlapi.md). To use the computer from a [terminal](../block/terminal.md), see the [serial interface card](../item/serial_interface_card.md).
