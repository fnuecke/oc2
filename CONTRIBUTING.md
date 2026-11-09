## Contributing

If you'd like to contribute that's awesome. As a starting point, here's my set of very rough design guidelines at the time of writing. When in doubt feel free to open an issue/discussion before working on a PR.

### Overview

The mod itself is structured fairly straightforward, with the bulk of it living in the common source tree. On a high level, it's split into:
- `api`: public facing interface of the mod other mods may reliably interact with. Internals may change at any time.
- `client`: pure client side code. Rendering, UIs.
- `common`: all the rest.

In the common subtree you'll find most of the core logic of the mod, and the item and block definitions.

The mod also uses stuff maintained outside this repository:
- [sedna](https://github.com/fnuecke/sedna), the emulator running the VMs.
- [OpenOS II](https://github.com/fnuecke/oc2-openos2), the kernel and userland of OpenOS II.
- [buildroot](https://github.com/fnuecke/buildroot), the RISC-V firmware and Linux disk image.
- [CP/M](https://github.com/fnuecke/sedna-cpm), the Z80 firmware and CP/M disk image.

### Design

1. **Immersion**. For lack of a better word. The mod should avoid breaking the fourth wall.
    1. In-game documentation reads in context, devices don't expose game internals like absolute coordinates.
    2. Physical interaction with the world should make sense, e.g. a bus interface should not be able to just move items around in an adjacent chest.
    3. As with all things, exceptions exist, e.g. block/item ids.
2. **Realism**. Computers are emulating real hardware.
    1. To some degree, knowledge should transfer from and to the real world.
    2. Going deeper is encouraged: building and using custom firmware or a custom OS should be possible without mod/datapack modifications.
3. **Flat Progression**. At least for now I'd like to focus on horizontal progression, rather than vertical.
    1. This means no complex tech tree of "tiered" hardware. One of my personal regrets with OC1.
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

When adding a new block/item, document it (on-device as well as ingame manual). If useful, add game tests for it.

### License

By contributing, you accept that the code is licensed under the MIT license.
