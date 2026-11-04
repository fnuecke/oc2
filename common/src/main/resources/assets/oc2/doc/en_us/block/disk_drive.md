# Disk Drive
![Taking it for a spin](block:oc2:disk_drive)

The disk drive provides an option for quick media exchange. [Floppy disks](../item/floppy.md) can be added and removed at runtime, unlike [hard drives](../item/hard_drive.md).

Computers *have to be shut down* before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.

## OpenOS II
See the [OpenOS II guide](../systems/openos2.md) for more on drives. Disk drives appear as `/dev/vdX` devices, following any installed hard drives. Floppies are mounted automatically, at `/mnt/<label>` or at `/mnt/<n>` without a label. Run `format /dev/vdX` to format a new floppy. Close open files before removing a floppy, to avoid data loss.

## Linux
See the [Linux guide](../systems/linux.md) for more on drives. Disk drives appear as `/dev/vdX` devices, following any installed hard drives. They are not automatically formatted or mounted, they will only appear as raw block devices:
- `mke2fs /dev/vdX` formats a floppy disk. Run this when first installing the disk. *Will erase data on floppy!*
- `mount /dev/vdX <mount directory>` mounts a floppy disk after formatting it. Make sure the directory you want to mount it to exists and is empty.
- `umount <mount directory>` unmounts a floppy disk. Make sure to run this before removing the floppy from the disk drive, to avoid data loss.

## CP/M
See the [CP/M guide](../systems/cpm.md) for more on drives. Drives are lettered in the order they are found, starting at `A:`. The disk the machine booted from always comes first.
