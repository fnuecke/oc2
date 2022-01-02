# OpenComputers II

OpenComputers II is a Minecraft mod adding virtual computers to the game. These computers run a virtual machine emulating a 64-Bit RISC-V architecture capable of booting Linux. On top of this, a high-level Lua API is provided to communicate with various devices in the game world. This enables adding virtual devices using a simple, Java-friendly API, without having to implement actual kernel drivers.

This mod is a successor to [OpenComputers]. At least in spirit. While many of the implementation details have changed quite dramatically, the core concepts of customizable hardware, persistence and sand-boxing are shared.

## Gameplay Documentation
For documentation on how the to get computers up and running, and how to use them, see the [documentation]. It is available as a manual item in the game.

## Development Documentation
While the mod isn't quite yet ready for release due to some remaining technical and usability issues, the API should be mostly stable at this point. For most people the high level device API will be sufficient, and is much more accessible. It centers around the [`RPCDevice`][RPC Device]. For a sample block implementation, see the [redstone interface]. For a sample item implementation, see the [sound card]. If you wish to dive deeper, and provide emulated hardware that requires a Linux driver, this centers around the [`VMDevice`][VM Device]. For a sample block implementation, see the [disk drive]. For a sample item implementation, see the [network card].

## Building
To build this project, credentials for the Github Package Repository must be set up once on your machine (see
[the documentation][GithubPackagesGradle] for more information). In short, you'll want to add your username and a
public access token with `read:packages` permissions into your `~/.gradle/gradle.properties`. The properties must be
named `gpr.user` and `gpr.key`.

[OpenComputers]: https://github.com/MightyPirates/OpenComputers
[RPC Device]: src/main/java/li/cil/oc2/api/bus/device/rpc/RPCDevice.java
[redstone interface]: src/main/java/li/cil/oc2/common/blockentity/RedstoneInterfaceBlockEntity.java
[sound card]: src/main/java/li/cil/oc2/common/bus/device/item/SoundCardItemDevice.java
[VM Device]: src/main/java/li/cil/oc2/api/bus/device/vm/VMDevice.java
[disk drive]: src/main/java/li/cil/oc2/common/blockentity/DiskDriveBlockEntity.java
[network card]: src/main/java/li/cil/oc2/common/bus/device/item/NetworkInterfaceCardItemDevice.java
[documentation]: src/main/resources/assets/oc2/doc/en_us/index.md
[GithubPackagesGradle]: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry
