# Datapack File Systems

Additional files can be provided to virtual machines using data packs. They are layered into the `/mnt/builtin`
9p file system every machine has access to.

A layer is a `.zip` file placed in the `file_systems` directory, with a JSON file of the same name next to it,
describing it.

The optional field `order` provides an integer, controlling the order in which layers are used. This allows
overriding files in other layers, if necessary.

Example, as `data/my_pack/file_systems/my_layer.json`, next to `my_layer.zip`:

```json
{
    "order": 100
}
```

There can be multiple such layers in the same directory.

The directory structure for this, when not using zipped datapacks, would look like this:

- datapacks/
    - my_data_pack/
        - pack.mcmeta
        - data/
            - my_pack/
                - file_systems/
                    - my_layer.json
                    - my_layer.zip

To preload storage media with images instead, see the `block_devices` directory.
