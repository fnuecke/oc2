It is possible to provide additional read-only data to virtual machines using data packs.

For this to work, a descriptor file must be placed in the `file_systems` directory. The descriptor must be a JSON file.
The file must have the following format:

```json
{
    "type": "<type>",
    "<type specific data>": "<type specific data>"
}
```

For example, to provide additional files for the layered 9pFS exposed to virtual machines, the type must be `layer` and
the extra field `location` must be provided, containing a resource location pointing to the ZIP containing the layer to
expose. Example:

```json
{
    "type": "layer",
    "location": "oc2:file_systems/scripts.zip"
}
```

An optional `order` field of type integer may be provided to control the order in which layers are used. This allows
overriding files in other layers, if necessary.