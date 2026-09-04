# OpenComputers II

OpenComputers II is a Minecraft mod adding virtual computers to the game. These computers run a virtual machine emulating a 64-bit **RISC-V** architecture capable of booting **Linux**, or an 8-bit **Z80** architecture running **CP/M 2.2**. On top of this, a high-level Lua API ([HLAPI]) is provided to communicate with various devices in the game world; on the Z80, a mid-level API ([MLAPI]) serves the same purpose but on an assembly level. This enables adding virtual devices using a simple, Java-friendly API, without having to implement actual kernel drivers.

This mod is a spiritual successor to [OpenComputers]. While many of the implementation details have changed quite dramatically, the concepts of customizable hardware, persistence and sand-boxing are still at the core of it.

Supported loaders are **Fabric** and **NeoForge**. The mod is published to [Curseforge](https://www.curseforge.com/minecraft/mc-mods/oc2) and [Modrinth](https://modrinth.com/project/opencomputers-ii).

## Gameplay Documentation

For documentation on how to get computers up and running, and how to use them, see the [documentation]. It is available as a manual item in the game.

## Development Documentation

The API should be mostly stable at this point. For most people the high level device API will be sufficient, and is much more accessible than adding actual emulated hardware. It centers around the [`RPCDevice`][RPC Device]. For a sample **block** implementation, see the [redstone interface]. For a sample **item** implementation, see the [sound card]. To support Z80 computers, use the [`IODevice`][IO Device]. The two are **not** mutually exclusive. Support both where appropriate.

If you wish to dive deeper, and provide emulated hardware that requires a Linux driver, this centers around the [`VMDevice`][VM Device]. For a sample block implementation, see the [projector]. For a sample item implementation, see the [network card].

### Sedna

The emulator used for running the virtual machines is [Sedna](https://github.com/fnuecke/sedna). It's a pure Java framework for emulating devices and architectures. Any issues/bugs/fixes/improvements that are explicitly applicable to it should be tracked on its own repo instead of here.

## Contributing

If you'd like to contribute that's awesome. As a starting point, here's my set of very rough design guidelines at the time of writing. When in doubt feel free to open an issue/discussion before working on a PR.

1. **Immersion**. For lack of a better word. The mod should avoid breaking the fourth wall.
   1. In-game documentation reads in context, devices don't expose game internals like absolute coordinates.
   2. Physical interaction with the world should make sense, e.g. a bus interface should not be able to just move items around in an adjacent chest.
   3. As with all things, exceptions exist, e.g. block/item ids.
2. **Realism**. Computers are emulating real hardware.
   1. To some degree, knowledge should transfer from and to the real world.
   2. Going deeper is encouraged: building and using custom firmware or a custom OS should be possible without mod/datapack modifications.
3. **Flat Progression**. At least for now I'd like to focus on horizontal progression, rather than vertical.
   1. This means no complex tech tree of "tiered" hardware.
   2. No explicit block/item being strictly better than another. Should always be a tradeoff.
   3. E.g. usually capability will drive up power consumption for now, not necessarily crafting costs/complexity.
   4. Prefer specific/focused devices to allow players actively making decisions when composing things.
4. **Sandboxing**. The mod should be safe to put on a server with untrusted players.
   1. Permission checks so devices can't be used to grief directly (robots breaking things for example).
   2. No **server** resource denial-of-service/resource exhaustion attacks: memory use limits, disk use limits, controlled network traffic, stable tick times. When in doubt, throttle.
   3. Indirectly griefing other players by exhausting **sandbox** resources is out of scope for now. E.g. one player spamming computers, causing others to not be able to start theirs.
5. **Maintainability**. Codebase should avoid extremely complex systems, beside the one core one, virtualization.
   1. Features should be compartmentalized. If this needs change to internal code design or architecture, that's fair game.
   2. Features should be tested. With gametests being so easy to add, this makes it a lot easier to catch regressions.
   3. Features mustn't need host configuration/dependencies. That was the whole point of writing an emulator in Java.
   4. Prefer good defaults over config values. In OC1 the config surface exploded to infinity, making it incredibly hard to test.

When adding a new block/item, document it (on-device as well as ingame manual). Preferably add game tests for it.

By contributing, you accept that the code is licensed under the MIT license.

## License

The code of the mod is licensed under [MIT](LICENSE). The art assets are public domain.

The mod vendors JCodec for sending the projector's frame buffer to clients, which is licensed under the [BSD 2-Clause](LICENSE-JCODEC).


[OpenComputers]: https://github.com/MightyPirates/OpenComputers
[RPC Device]: common/src/main/java/li/cil/oc2/api/bus/device/rpc/RPCDevice.java
[IO Device]: common/src/main/java/li/cil/oc2/api/bus/device/io/IODevice.java
[redstone interface]: common/src/main/java/li/cil/oc2/common/blockentity/RedstoneInterfaceBlockEntity.java
[sound card]: common/src/main/java/li/cil/oc2/common/bus/device/rpc/item/SoundCardItemDevice.java
[VM Device]: common/src/main/java/li/cil/oc2/api/bus/device/vm/VMDevice.java
[projector]: common/src/main/java/li/cil/oc2/common/bus/device/vm/block/ProjectorDevice.java
[network card]: common/src/main/java/li/cil/oc2/common/bus/device/vm/item/NetworkInterfaceCardDevice.java
[documentation]: common/src/main/resources/assets/oc2/doc/en_us/index.md
[HLAPI]: common/src/main/resources/assets/oc2/doc/en_us/hlapi.md
[MLAPI]: common/src/main/resources/assets/oc2/doc/en_us/mlapi.md
