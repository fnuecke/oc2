# OpenComputers II

[![build](https://img.shields.io/github/actions/workflow/status/fnuecke/oc2/build.yml?label=build)](https://github.com/fnuecke/oc2/actions/workflows/build.yml)
[![game tests](https://img.shields.io/github/actions/workflow/status/fnuecke/oc2/test-report.yml?label=game%20tests)](https://github.com/fnuecke/oc2/actions/workflows/test-report.yml)
[![curseforge](https://img.shields.io/curseforge/dt/437654?label=curseforge&color=f16436&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/oc2)
[![modrinth](https://img.shields.io/modrinth/dt/opencomputers-ii?label=modrinth&color=1bd96a&logo=modrinth&logoColor=white)](https://modrinth.com/project/opencomputers-ii)
![loaders](https://img.shields.io/badge/loaders-Fabric%20%7C%20NeoForge-blueviolet)

OpenComputers II is a Minecraft mod adding virtual computers to the game. These computers run a virtual machine emulating a 64-bit **RISC-V** architecture capable of booting **OpenOS II** and **Linux**, or an 8-bit **Z80** architecture running **CP/M 2.2**. On top of this, a high-level API ([HLAPI]) is provided to communicate with various devices in the game world, geared mostly towards Lua. A mid-level API ([MLAPI]) serves the same purpose but on an assembly-friendly level. This allows adding virtual devices using a simple, Java-friendly API, without having to implement actual kernel drivers for most devices.

This mod is a spiritual successor to [OpenComputers]. While many of the implementation details have changed quite dramatically, the concepts of customizable hardware, persistence and sand-boxing are still at the core of it.

## Gameplay Documentation

For documentation on how to get computers up and running, and how to use them, see the [documentation]. It is also available as a manual item in the game.

## Development Documentation

The [`docs`](docs) directory holds a high level overview of e.g. the device API for mods and the data pack extension points. The [`examples`](examples) directory holds minimal mods using the device API. The [API Javadoc] documents the mod's API types.

## LLM Use Disclaimer

Use of LLMs on this project includes: gradle, automated testing, research, debugging, verification of documentation, gradle, refactoring, prototyping, gradle.

[OpenComputers]: https://github.com/MightyPirates/OpenComputers
[documentation]: https://fnuecke.github.io/oc2/manual/
[API Javadoc]: https://fnuecke.github.io/oc2/javadoc/
[HLAPI]: https://fnuecke.github.io/oc2/manual/en_us/hlapi.html
[MLAPI]: https://fnuecke.github.io/oc2/manual/en_us/mlapi.html
