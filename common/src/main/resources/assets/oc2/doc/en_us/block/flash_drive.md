# Flash Drive
![Savior of the Universe](block:oc2:flash_drive)

The flash drive exposes [flash memory](../item/flash_memory.md) to a [computer](computer.md) as a plain block device. Similar to the [disk drive](disk_drive.md) allows access to floppies, this device allows to access flash memory. In particular, this enables use of custom firmware.

Flash memory can be inserted and removed at runtime. The drive itself cannot: computers *have to be shut down* before installing or removing it.

On a Linux system, the drive appears as a `/dev/vdX` device, following any installed hard drives, just like a floppy disk drive. It is a raw block device.

To write your firmware to the flash memory, *erasing what was there before*:
  `dd if=<firmware file> of=/dev/vdX bs=512 conv=fsync`
