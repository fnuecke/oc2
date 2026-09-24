# Examples

Minimal mods using the [device API]. Each directory is a standalone Gradle build, based on the [NeoForge MDK] or the
[Fabric template].

## Building

The examples build against the oc2 sources in this repository, via `includeBuild` in `settings.gradle.kts`.
Removing that part makes them build against the published oc2 instead.

```sh
cd examples/card-neoforge
./gradlew runClient
```

Fabric Loom reads mod jars while configuring the build, before the included oc2 build runs. Build oc2's Fabric
jar first, from the repository root:

```sh
./gradlew :fabric:remapJar
```

[device API]: ../docs/api.md
[NeoForge MDK]: https://neoforged.net/mod-generator/
[Fabric template]: https://fabricmc.net/develop/template/
