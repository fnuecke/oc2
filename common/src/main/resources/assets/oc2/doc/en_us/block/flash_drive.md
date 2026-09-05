# Flash Drive
![Savior of the Universe](block:oc2:flash_drive)

The flash drive exposes a [flash memory](../item/flash_memory.md) chip to a [computer](computer.md) as a plain block device. Similar to the [disk drive](disk_drive.md) allows access to floppies, this one exists to access flash memory: it is how you put your own firmware onto a chip.

Flash memory can be inserted and removed at runtime. The drive itself cannot: computers *have to be shut down* before installing or removing it.

On a Linux system, the chip appears as a `/dev/vdX` device, following any installed hard drives, exactly like a floppy in a disk drive. It is a raw block device with no filesystem on it, and there is no point putting one there.

To write your firmware to the flash memory, *erasing what was there before*:
  `dd if=<firmware file> of=/dev/vdX bs=512 conv=fsync`
