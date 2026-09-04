# Datapack Block Devices

Storage media can be preloaded with data provided by data packs, so packs can ship their own disk images and
firmware.

An image is a raw `.bin` file, placed in a directory naming the medium it is for:

- `block_devices/hdd` for hard drives.
- `block_devices/floppy` for floppies.
- `block_devices/flash` for flash memory, i.e. firmware.

Next to it sits a JSON file of the same name, describing it.

The optional field `name` provides the display name. If not specified, the display name will be "???".

The optional field `color` provides a dye color name, such as `cyan`. Items preloaded with this image are tinted
with it. If not specified, items keep their own default color.

Example, as `data/my_pack/block_devices/hdd/my_disk.json`, next to `my_disk.bin`:

```json
{
    "name": "My Disk Image",
    "color": "cyan"
}
```

Images are identified by their own path, so the one above is `my_pack:block_devices/hdd/my_disk.bin`. The images
the mod itself provides use the same scheme, which means a data pack can replace one by shipping an image at the
same path:

- `oc2:block_devices/hdd/buildroot.bin`
- `oc2:block_devices/floppy/cpm.bin`
- `oc2:block_devices/flash/riscv.bin`
- `oc2:block_devices/flash/z80.bin`

A medium holding an image is the size of that image, so the size of the item it sits on is not a limit but a
selector: hard drives come in several sizes, and an `hdd` image is offered on the smallest one it fits on, and
only on that one. Floppies and flash memory have a single item each, so there is nothing to select.

This only affects which item the image is offered on in the creative tab. A data pack or loot table referencing
the image directly can put it on any medium, at whatever size the image is.

Note that firmware is what the processor starts executing when the computer powers on, so it must be code the
installed processor can run.

The directory structure for this, when not using zipped datapacks, would look like this:

- datapacks/
    - my_data_pack/
        - pack.mcmeta
        - data/
            - my_pack/
                - block_devices/
                    - hdd/
                        - my_disk.json
                        - my_disk.bin
