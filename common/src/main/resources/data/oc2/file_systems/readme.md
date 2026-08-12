# Datapack File Systems

It is possible to provide additional data to virtual machines using data packs.

For this to work, a descriptor file must be placed in the `file_systems` directory. The descriptor must be a JSON file.
The file must have the following format:

```json
{
    "type": "<type>",
    "<type specific data>": "<type specific data>"
}
```

Supported types are:

- `block`, for custom hard drives based on file system images.
- `layer`, for ZIP files to be layered into the `/mnt/builtin` 9p file system.

There can be multiple such descriptor files in the same directory.

The directory structure for this, when not using zipped datapacks, would then look like this, for example:

- datapacks/
    - my_data_pack/
        - pack.mcmeta
        - data/
            - oc2/
                - file_systems/
                    - my_block_descriptor.json
                    - my_block.ext2
                    - my_layer_descriptor.json
                    - my_layer.zip

## `layer`

To provide additional files for the layered 9pFS exposed to virtual machines, the type must be `layer`.

The required field `location` provides a resource location, pointing to the ZIP file containing the data for the layer.

The optional field `order` provides an integer, controlling the order in which layers are used. This allows overriding
files in other layers, if necessary.

Example:

```json
{
    "type": "layer",
    "location": "oc2:file_systems/my_layer.zip"
}
```

## `block`

To provide additional hard disks based on raw file system images, they type must be `block`.

The required field `location` provides a resource location, pointing to the file that is used as the raw disk image.

The optional field `name` provides the display name. If not specified, the display name will be "???".

Example:

```json
{
    "type": "block",
    "name": "My Disk Image",
    "location": "oc2:file_systems/my_block.ext2"
}
```
