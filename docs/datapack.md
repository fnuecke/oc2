# Data Packs

OpenComputers II reads four directories from data packs. Any namespace works.

- `block_devices` preloads storage media with disk images and firmware.
- `file_systems` adds files to the `/mnt/builtin` file system RISC-V machines mount.
- `cpm` adds files to the CP/M boot floppy.
- `item_tag_filters` controls which item data a program running on a computer can see.

The directory structure, when not using zipped data packs:

- datapacks/
    - my_data_pack/
        - pack.mcmeta
        - data/
            - my_pack/
                - block_devices/
                    - hdd/
                        - my_disk.json
                        - my_disk.bin
                - file_systems/
                    - my_layer.json
                    - my_layer.zip
                - cpm/
                    - mytool.z80
                - item_tag_filters/
                    - my_filter.json

## Block Devices

Storage media can be preloaded with data provided by data packs, so packs can ship their own disk
images and firmware.

An image is a raw binary `.bin` file, placed in a directory named after the medium it is for:

- `block_devices/hdd` for hard drives.
- `block_devices/floppy` for floppies.
- `block_devices/flash` for flash memory, i.e. firmware.

Next to it sits a JSON file of the same name, describing it.

The optional field `name` provides the display name. If not specified, the display name will be "???".

The optional field `color` provides a dye color name, such as `cyan`. Items preloaded with this image
are tinted with it. If not specified, items keep their own default color.

Example, as `data/my_pack/block_devices/hdd/my_disk.json`, next to `my_disk.bin`:

```json
{
    "name": "My Disk Image",
    "color": "cyan"
}
```

Images are identified by their own path, so the one above is `my_pack:block_devices/hdd/my_disk.bin`.
The images we provide are registered in code rather than shipped as files, but they use the same ids,
and a data pack image wins over a registered one. So shipping an image at one of these paths replaces it:

- `oc2:block_devices/hdd/sedna.bin`
- `oc2:block_devices/floppy/cpm.bin`
- `oc2:block_devices/flash/riscv.bin`
- `oc2:block_devices/flash/z80.bin`

The image is copied onto an item the first time that item is mounted. Changing the image afterwards
does not update media that was initialized/already has copied data.

### Sizes

Hard drives and floppies will have the exact size of their image data. Flash memory is different: it
keeps the item's own size, and an image that does not fit fails when the machine starts.

In the creative tab, an `hdd` image is offered on the smallest hard drive it fits on, and only on that
one. An image too large for any of them is offered on the largest. A `floppy` image larger than a
floppy is not offered at all. Flash memory is offered whatever the size, since the check happens at
mount.

This only affects the creative tab. A data pack or loot table referencing an image directly can put it
on any medium.

Note that firmware is what the processor starts executing when the computer powers on, so it must be
code the installed processor can run.

## File Systems

Additional files can be provided to virtual machines using data packs. They are layered into the
`/mnt/builtin` 9p file system, which the RISC-V Linux system mounts at boot. Z80 machines have no 9p
device, to include additional files here, see the next section.

A layer is a `.zip` file placed in the `file_systems` directory, with a JSON file of the same name next
to it, describing it. The descriptor is what is searched for, so a zip without one is ignored, as is
an empty zip.

The optional field `order` provides an integer, controlling the order in which layers are used.
**Higher wins**: where two layers hold the same path, the one with the larger `order` is the one
programs see. Layers without an `order` sit at zero.

Example, as `data/my_pack/file_systems/my_layer.json`, next to `my_layer.zip`:

```json
{
    "order": 100
}
```

There can be multiple such layers in the same directory.

The shipped Linux system runs `/mnt/builtin/init.d/S??*` at boot and puts `/mnt/builtin/bin` on the
`PATH` of login shells, which is how we ship our own Lua and MicroPython libraries. Both are conventions
of that default Linux system, so a pack booting a custom root filesystem would need to do this manually.

## CP/M Files

Files in the `cpm` directory are written onto the CP/M boot floppy, so packs can ship their own tools,
includes and examples for Z80 machines. The disk is composed fresh on every resource reload.

There is no descriptor. Every file in the directory is offered, but CP/M is strict about what it can
hold, and a file it rejects is dropped with an error in the log rather than failing the reload:

- The name must already be a valid CP/M 8.3 name in printable ASCII. It is validated, not converted,
  so `my_program.z80` is rejected outright for its ten-character name rather than being truncated.
- A file may be at most 16 KB.
- Names must be unique. The file whose full resource path sorts first is the only one included.
- The disk has finite directory entries and blocks, and fills up.

Files that look like text have their line endings converted to CP/M convention and an end-of-file
marker appended. Anything else is written unchanged.

Only the file name is used. The namespace and any subdirectory under `cpm` are dropped, so
`my_pack:cpm/tools/redstn.z80` lands as `REDSTN.Z80`.

We ships `ocapi.inc`, `redstn.z80` and `serchat.z80` this way.

## Item Tag Filters

When a program on a computer reads an item stack, it does not see the whole stack. Item data is passed
through an allow-list, so a filter has to name a path before a program can read it. This keeps
unbounded or private data out of the guest, and keeps what a program sees stable.

A filter is a JSON file anywhere under `item_tag_filters`, including in subdirectories.

The field `tags` lists the paths to allow. A path names components or nested tags, separated by dots,
as they appear in the item's serialized form. A filter with no `tags` is not an error; it simply does
nothing. Because dots separate path segments, a key containing a dot cannot be named.

The optional field `item` restricts the filter to a single item, by id. Without it, the filter applies
to every item.

Example, as `data/my_pack/item_tag_filters/my_filter.json`:

```json
{
    "item": "my_mod:my_item",
    "tags": [
        "components.minecraft:custom_name",
        "components.minecraft:custom_data.my_mod.charge"
    ]
}
```

Every loaded filter is applied and the results are merged, so packs add to the allow-list rather than
replacing it. Apart from the stack size, which is always sent, a path no filter names is not visible to
programs.
