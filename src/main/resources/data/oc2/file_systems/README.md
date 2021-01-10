It is possible to provide additional read-only data to virtual machines using data packs.

For this to work, a descriptor file must be placed in the `file_systems` directory. The descriptor must be a JSON file
with a name ending in `.fs.json`. The file must have the following format:

```json
{
    "type": "<type>",
    "<type specific data>": "<type specific data>"
}
```

For example, to provide additional files for the layered 9pFS exposed to virtual machines, the type must be `virtio-9p`
and the extra field `location` must be provided, containing a resource location pointing to the root of the path to
expose. Example:

```json
{
    "type": "virtio-9p",
    "location": "oc2:file_systems/scripts"
}
```

All files in paths exposed this way will be flagged non-executable by default. To mark a file executable, place
a `.mcmeta` file with the following content next to:

```json
{
    "attributes": {
        "is_executable": true
    }
}
```
