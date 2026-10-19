# Floppy Controller

Z80 only. Used for Disk Drive blocks and the floppy slot in robots. Found through
the [enumeration window](z80.md#enumeration-window). On RISC-V floppies are made available as virtio block devices. So
this is a rare case where a different device is added to the bus based on the current architecture. Linux doesn't
support this controller out of the box, so this is a compromise.

Class `0x03`, name `FLOPPY`. A WD1793-style controller with five ports and a single drive, unit 0. Reports not ready
while empty. The robot's floppy slot only appears as a controller while it holds a floppy. Command and status bit
constants used by the shipped ROM and BIOS:
[wd1793.inc](https://github.com/fnuecke/sedna-cpm/blob/main/src/main/asm/wd1793.inc), also on the CP/M boot floppy.

| Offset | Read                          | Write                                                                      |
|--------|-------------------------------|----------------------------------------------------------------------------|
| 0      | Status, clears the IRQ flag   | Command, clears the IRQ flag                                               |
| 1      | Track register                | Track register                                                             |
| 2      | Sector register, 1-based      | Sector register                                                            |
| 3      | Data                          | Data                                                                       |
| 4      | Flags: `0x80` IRQ, `0x40` DRQ | System: bits 0-1 unit, bit 4 side, bit 2 rising edge resets the controller |

While busy, commands other than force interrupt and writes to the track and sector registers are ignored.

The track register is the head position. There is no separate physical track and no ID field compare, so writing the
track register is a seek without bounds check.

Writing the system register with bit 2 set after it was clear resets the controller: track 0, sector register 1, system
register `0x04`. A reset pulse is `0x00` then `0x04`.

## Geometry

This is the only geometry, the media type in the enumeration window's attributes is always `0`. One side, 128 tracks, 32
sectors per track, 128 bytes per sector: 512 KiB. Sector `s` of track `t` is at byte offset `(t * 32 + s - 1) * 128` of
the disk image. The geometry is fixed whatever the image size. Sectors past the end of a smaller image report record not
found.

## Disk Layout

The layout of the shipped CP/M disks. A blank floppy is filled with `0xE5`, an empty CP/M directory without a boot
descriptor.

| Tracks | Content                                                                                                                        |
|--------|--------------------------------------------------------------------------------------------------------------------------------|
| 0-1    | Boot area, 8 KiB. Track 0, sector 1 holds the [boot descriptor](z80.md#boot-descriptor), the system image starts with sector 2 |
| 2-127  | CP/M data area, 252 blocks of 2048 bytes. Blocks 0 and 1 hold the directory, 128 entries                                       |

There is no sector skew. The data area holds 500 KiB of files. The BIOS uses this disk parameter block for every drive:

| Field        | Value          |
|--------------|----------------|
| `SPT`        | 32             |
| `BSH`, `BLM` | 4, 15          |
| `EXM`        | 1              |
| `DSM`        | 251            |
| `DRM`        | 127            |
| `AL0`, `AL1` | `0xC0`, `0x00` |
| `CKS`        | 32             |
| `OFF`        | 2              |

To work with disk images on the host, use [cpmtools](http://www.moria.de/~michael/cpmtools/) with this definition,
also found in [sedna-cpm](https://github.com/fnuecke/sedna-cpm/blob/main/src/main/diskdefs/diskdefs):

```
diskdef sedna
  seclen 128
  tracks 128
  sectrk 32
  blocksize 2048
  maxdir 128
  skew 1
  boottrk 2
  os 2.2
end
```

## Commands

| Command                               | Code                   | Behavior                                                                                                                                      |
|---------------------------------------|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| Restore                               | `0x00`                 | Track register to 0                                                                                                                           |
| Seek                                  | `0x10`                 | Track register to the value in the data register                                                                                              |
| Step, step in, step out               | `0x20`, `0x40`, `0x60` | Step repeats the last direction, initially in. With `0x10` set, track register plus or minus one, without, only checks bounds and sets status |
| Read sector                           | `0x80`                 | `0x10` flag: continue to the end of the track                                                                                                 |
| Write sector                          | `0xA0`                 | `0x10` flag: continue to the end of the track                                                                                                 |
| Force interrupt                       | `0xD0`                 | Ends a transfer, discarding a partially written sector. `0x08` flag raises IRQ                                                                |
| Read address, read track, write track | `0xC0`, `0xE0`, `0xF0` | Unsupported, report record not found                                                                                                          |

Of the low nibble, type I commands honor only `0x08`, which sets head loaded in status. The step update flag `0x10` is
part of the high nibble (`0x30`, `0x50`, `0x70`). Type II commands take the side compare flag `0x02` with the side in
`0x08`. Force interrupt honors only `0x08`. Stepping outside tracks 0-127 is a seek error.

Commands complete immediately (no emulated delays). A transfer starts with DRQ set and moves one byte per data register
access. A written sector is stored when its 128th byte arrives. A command ends with busy clear and IRQ set. Reading
register 4 255 times during a transfer without touching the data register aborts it with lost data. Poll status instead.
Changing the medium aborts a transfer with lost data. Writing to protected media fails at once with `0x40`.

A failed read returns stale bytes or `0xFF` from the data register. Check status after every transfer.

## Status

| Bit    | Type I (restore, seek, step) | Type II (read, write) |
|--------|------------------------------|-----------------------|
| `0x80` | Not ready, no disk           | Not ready, no disk    |
| `0x40` | Write protected              | Write protected       |
| `0x20` | Head loaded                  |                       |
| `0x10` | Seek error                   | Record not found      |
| `0x04` | Track 0                      | Lost data             |
| `0x02` | Index                        | DRQ                   |
| `0x01` | Busy                         | Busy                  |
