# The OpenComputers II API

Welcome to the API of `oc2`, fellow developer! This document will hopefully provide a sufficient overview of what
integrations this API allows, and how to best implement them. The primary purpose of the API is to allow other mods to
implement their own devices, to be used by the computers in this mod.

See the generated [Javadoc] for per type/interface documentation.

## Loaders and Packages

`oc2` is built for both Fabric and NeoForge. The API is split accordingly:

- [li.cil.oc2.api] is loader-independent. Devices, providers, registry keys and the annotations live here.
- [li.cil.oc2.api.fabric] holds the Fabric `BlockApiLookup`/`ItemApiLookup`/`EntityApiLookup` instances.
- [li.cil.oc2.api.neoforge] holds the matching NeoForge `BlockCapability`/`ItemCapability`/`EntityCapability` instances.

The two platform packages mirror each other: every entry in `Lookups` has a counterpart under the same name in
`Capabilities`, and both use the same resource locations.

## High-level and Mid-level Devices

The core of the HLAPI and MLAPI device system are the [RPCDevice] and [IODevice] interfaces. An `RPCDevice` defines a
list of [RPCMethods], an `IODevice` defines a list of [IOMethods], which represent the methods that can be called on the
device.

These are the suggested types of device to add, and allows easily exposing methods in your classes to virtual machines.

> [!NOTE]
> If you've seen the APIs of OpenComputers or ComputerCraft before, the `RPCDevice` should feel fairly familiar.

### Threading

Methods run on the main server thread by default. By default, exposed methods will be synchronized to the server thread.
This is what you want if your logic does anything with the level or stuff in it. Opting out makes calls much cheaper,
since a synchronized call costs at least one tick, but is only safe for methods that touch nothing but their own state.
Do **not** try to synchronize against the server thread here. VMs are joined to the server thread each tick, so you'd
just introduce a deadlock.

To have methods called asynchronously, either declare them as non-synchronized in your `RPCMethod`/`IOMethod`, or when
using the annotations, by using `@Callback(synchronize = false)`/`IOCallback(synchronize = false)`.

### The `ObjectDevice`

It is perfectly fine to implement these interfaces manually. There is a more convenient way, however, when adding a
device explicitly for this mod, in the form of the [ObjectDevice]. This class allows wrapping a Java object as an
`RPCDevice`/`IODevice` (can be both at the same time).

Methods annotated with [Callback] expose functionality to RISC-V computers, through teh high-level API. The class level
annotation with [RPCDeviceDescription] is optional, as the high-level API also supports a level of reflection to
inspect devices.

Methods annotated with [IOCallback] expose functionality to Z80 computers, through the mid-level API. The class then
must be annotated with a [IODeviceDescription], to allow machines to identify the device type, since there's no other
form of type reflection in the mid-level API.

The class and the annotated methods must be public.

### Type Names

In addition to methods, `RPCDevices` provide a list of "type names". These names are metadata, that can be used by
programs running in the virtual machines to identify devices. These should be clear and unique, to avoid confusion
between device types. For example, "machine" would probably be a little too generic, whereas "redstone_furnace" would
probably be a little better. Note that for all `BlockEntities` providing devices, the registry name of their
`BlockEntityType` is automatically added to the list of type names; for blocks without a block entity, the block's
registry name is used instead. Equally, for all `Items` providing devices, their registry name is automatically added to
the list of type names.

### Method Name Collisions

All `RPCDevices` found for a particular `BlockEntity` or `Item` will be merged, and present as one singular `RPCDevice`
to the virtual machine. This means that not only type names are merged, but `RPCMethodGroups` are merged into a single
list as well. In most cases, it is fine to return each `RPCMethod` as its own `RPCMethodGroup`. For this reason, the
`RPCMethod` interface extends the `RPCMethodGroup` interface.

The system supports method overloading to some degree. `RPCMethodGroups` with a matching method name are queried one by
one for an overload matching the invocation's parameters. `RPCMethods` provide a default implementation for this, using
the declared parameter types to determine if they match.

However, RPCs are passed from VM to Java as JSON messages, so some overloads, that are clearly different on the Java
side, may lead to ambiguity. Specifically, in cases where one JSON serialization can be deserialized into different
types. Most problematic in this area are `null` values, since they match any object type parameter.

> [!NOTE]
> The system does a best-effort attempt: it will try deserializing parameters for ambiguous overloads one after
> the other, until deserialization for all parameter types succeeds.

To avoid ambiguity, it is recommended to pick clear and unique method names where reasonable. This is particularly true
for generic `RPCDevices`, e.g. devices providing access to common capabilities, which may be provided by various
`BlockEntities`. An example for this are the built-in devices for energy storage.

For more control, `RPCMethodGroups` may implement custom overload resolution via `findOverload(RPCInvocation)`.

### Custom Parameter Types

Method parameters and return values are passed to and from the VM as JSON. Primitives, strings, arrays and plain data
classes work out of the box. Types that need special handling require a Gson type adapter, described by the
[RPCTypeAdapter] record. Register one with the `Registries.RPC_TYPE_ADAPTER` registry, exactly as you would a device
provider. `ItemStack` and `Direction` are registered this way already.

Adapters apply in registration order, so registering one for a type the mod already covers replaces it. An adapter may
implement Gson's `JsonSerializer`, `JsonDeserializer` or both; only implement the direction you need. The built-in
`ItemStack` adapter serializes only, because stacks are handed to the guest but never accepted from it.

### Device Lifecycle

Where needed, the optional interface methods `mount(RPCBusContext)`, `unmount(RPCBusContext)` and `dispose()` may be
implemented, to react to device lifecycle events. This can be useful in case some state needs to be initialized or
reset, when the computer starts or stops, or the device is connected to or disconnected from a computer.

These methods are called in the following cases:

- `mount(context)` is called when a device is added to a running computer, or the computer it was added to starts
  running. It is also called when a computer resumes running after the chunk it sits in is loaded.
- `unmount(context)` is called when the computer suspends because its chunk is unloaded or the server stops, when the
  computer stops, and when the device is removed from a running computer. If `mount()` was called, `unmount()` is
  guaranteed to follow, with the same `RPCBusContext`.
- `dispose()` is called when the computer stops or the device is removed, after `unmount()`. It is not a terminal state:
  a device still on the bus when its computer starts again is mounted again, so it must leave itself usable. Releasing
  resources here is fine as long as they can be re-acquired on the next mount.

Note that `unmount()` is called both for suspending and stopping. When stopping, `dispose()` is called as well.

The `RPCBusContext` is the device's handle on the computer it was mounted in. A device reachable from several computers,
such as a block entity with computers on two sides, is mounted once per computer and receives a distinct context for
each. Thus, a device may receive more than one context; keep them in a `Set`, and remove entries passed by `unmount()`.

This can be useful for various things. For example:

- Setting a flag in the block the device is associated with.
    - Set the flag in `mount()`.
    - Unset the flag in `unmount()`.
- Track out-of-minecraft resources, such as a file with extra data.
    - Create and open the file in `mount()`.
    - Close the file in `unmount()`.
    - Delete the file in `dispose()`, and be ready to create it again on the next `mount()`.

### Events

`RPCBusContext.sendEvent(type, data)` raises an event in the guest, where scripts receive it through `waitEvent`. The
context handles device id mapping, so that the guest can tell which device raised the event. Emit events sparingly,
usually on some state change: the event queue is shared by every device on the computer, and has a size limit. Events
that follow once the queue is full are dropped.

`sendEvent` may be called from any thread. `data` is serialized on the calling thread, so only pass game objects such as
an `ItemStack` from the server thread. Binary payloads (`byte[]`) are not supported in events; such an event is logged
and dropped. After `unmount()`, `sendEvent` returns `false`.

## The `BlockDeviceProvider` and `ItemDeviceProvider`

So let's say you have some `RPCDevice` at hand (or a `VMDevice`). Now you want the computer to use it. The core
functionality that makes `Devices` available to the mod are the [BlockDeviceProvider] and the [ItemDeviceProvider]
interfaces.

There exists a registry for each, with which all block and item providers must be registered. These registries are
queried to collect devices for a given block in the world, or an item in a machine inventory.

The two interfaces differ in return type, deliberately. `BlockDeviceProvider` returns an [Invalidatable] device: a block
can drop its device at any time, and invalidating the value makes the bus drop it and rescan. `ItemDeviceProvider`
returns a plain `Optional<ItemDevice>`, because an item device is owned by the machine itself, so it can only become
invalid due to slot changes, which are local/observable directly.

### Registering Providers

[Registries] holds the `ResourceKey` of every registry the mod creates. Register with them the way your loader registers
with any other registry.

On NeoForge, a `DeferredRegister` on the mod event bus is enough, see [Integration][NeoForge provider registration].

On Fabric the registries are created by `oc2` itself, so they only exist once `oc2` has initialized. Use the
`oc2:registration` entrypoint to ensure correct ordering. In your entrypoint, look the registry up by its key, see
[Registration][Fabric provider registration] and its declaration in [fabric.mod.json][Fabric entrypoint declaration].

### Block Devices

Block devices are queried for all blocks adjacent to a `Bus Interface` that is connected to some computer via some `Bus
Cable` and another `Bus Interface`. Connected `Bus Cables` with attached `Bus Interfaces` define a [DeviceBus].
Computers collect all devices attached to the `DeviceBus` and make them available to the virtual machine they run. Each
registered `BlockDeviceProvider` is queried for a block in question, and the found `RPCDevices` are aggregated into one
`RPCDevice` proxy.

> [!NOTE]
> `BusInterfaces` look for `Devices` using `BlockDeviceProviders`.

The mod comes with a set of convenience `BlockDeviceProviders`, which enable offering devices in various ways. This
means you don't necessarily have to implement your own provider. The following built-in providers exist:

- `BlockEntities` are queried for the device capability (`Capabilities.Device.BLOCK` on NeoForge, `Lookups.Device.BLOCK`
  on Fabric). If there is one, the returned device is used. See the [NeoForge][NeoForge block entity example] and
  [Fabric][Fabric block entity example] examples.
    - This allows optional support for this mod, based on whether it is present or not.
- `Blocks` and `BlockEntities` are scanned for `Callbacks`. If there are any, they are wrapped in an `ObjectDevice`.
    - This implies a hard dependency on this mod, due to the use of the `Callback` annotation in your `Block`
      /`BlockEntity` code.

### Item Devices

Item devices are queried for items inserted into computers and robots. For each `ItemStack` in a device slot, each
`ItemDeviceProvider` is queried for the item in question, and the found `RPCDevices` are aggregated into one `RPCDevice`
proxy.

> [!NOTE]
> Such items must be tagged with the slot type they fit into, or they cannot be placed into computers and robots.

The card example has a [provider][card provider] and the [slot tag][card slot tag].

[DeviceTypes] lists the built-in slot types as `Supplier<DeviceType>`, resolved from the registry on first use:
`DeviceTypes.CARD.get()`. Calling `get()` before the mod has registered its device types throws, so do not resolve them
from a static initializer of your own.

## Other Capabilities

Besides devices, the mod looks for and provides a handful of other capabilities. Each has a Fabric lookup and a NeoForge
capability under the same name:

| Purpose                                  | Fabric                     | NeoForge                        |
|------------------------------------------|----------------------------|---------------------------------|
| Expose a block/item as a device          | `Lookups.Device`           | `Capabilities.Device`           |
| Join a device bus, e.g. a custom cable   | `Lookups.DeviceBusElement` | `Capabilities.DeviceBusElement` |
| Emit a redstone signal for a device      | `Lookups.RedstoneEmitter`  | `Capabilities.RedstoneEmitter`  |
| Exchange ethernet frames with connectors | `Lookups.NetworkInterface` | `Capabilities.NetworkInterface` |
| Let installed modules reach their host   | `Lookups.Robot`            | `Capabilities.Robot`            |

Fabric has no built-in entity lookups for energy or item storage, which robots need. We declare our own:
`EnergyStorage.ENTITY` and `ItemStorage.ENTITY` in `li.cil.oc2.api.fabric`; the same classes re-export the standard
`SIDED` and `ITEM` lookups so all three can be reached from one place. On NeoForge the corresponding NeoForge
capabilities already cover entities, and are used directly.

## The `VMDevice`

`VMDevices` are low-level, memory-mapped devices, emulating "real" hardware, and thus requiring driver support by the
operating system running in the virtual machines.

> [!NOTE]
> `VMDevices` are very low-level, and something most people can ignore.

The core of the `VMDevice` system is the [VMDevice] interface itself. It defines a proxy used to load and unload actual
emulated hardware. `VMDevices` use the [VMContext] to bind hardware to the virtual machine upon initialization. This
typically includes reserving an address block in memory, possibly hooking up interrupts and reserving host memory from
the memory tracker. In most cases, `VMDevices` will add a `MemoryMappedDevice` to the `MemoryMap`, an interface used by
[Sedna], the VM implementation used to run the computers in this mod.

[Javadoc]: https://fnuecke.github.io/oc2/javadoc/
[li.cil.oc2.api]: ../common/src/main/java/li/cil/oc2/api
[li.cil.oc2.api.fabric]: ../fabric/src/main/java/li/cil/oc2/api/fabric
[li.cil.oc2.api.neoforge]: ../neoforge/src/main/java/li/cil/oc2/api/neoforge
[RPCDevice]: ../common/src/main/java/li/cil/oc2/api/bus/device/rpc/RPCDevice.java
[IODevice]: ../common/src/main/java/li/cil/oc2/api/bus/device/io/IODevice.java
[RPCMethods]: ../common/src/main/java/li/cil/oc2/api/bus/device/rpc/RPCMethod.java
[ObjectDevice]: ../common/src/main/java/li/cil/oc2/api/bus/device/object/ObjectDevice.java
[Callback]: ../common/src/main/java/li/cil/oc2/api/bus/device/object/Callback.java
[IOCallback]: ../common/src/main/java/li/cil/oc2/api/bus/device/io/IOCallback.java
[RPCDeviceDescription]: ../common/src/main/java/li/cil/oc2/api/bus/device/object/RPCDeviceDescription.java
[IODeviceDescription]: ../common/src/main/java/li/cil/oc2/api/bus/device/io/IODeviceDescription.java
[RPCTypeAdapter]: ../common/src/main/java/li/cil/oc2/api/bus/device/rpc/RPCTypeAdapter.java
[BlockDeviceProvider]: ../common/src/main/java/li/cil/oc2/api/bus/device/provider/BlockDeviceProvider.java
[ItemDeviceProvider]: ../common/src/main/java/li/cil/oc2/api/bus/device/provider/ItemDeviceProvider.java
[Invalidatable]: ../common/src/main/java/li/cil/oc2/api/util/Invalidatable.java
[Registries]: ../common/src/main/java/li/cil/oc2/api/util/Registries.java
[NeoForge provider registration]: ../examples/third-party-block-neoforge/src/main/java/com/example/thirdpartyblock/Integration.java
[Fabric provider registration]: ../examples/third-party-block-fabric/src/main/java/com/example/thirdpartyblock/Registration.java
[Fabric entrypoint declaration]: ../examples/third-party-block-fabric/src/main/resources/fabric.mod.json
[DeviceBus]: ../common/src/main/java/li/cil/oc2/api/bus/DeviceBus.java
[NeoForge block entity example]: ../examples/block-entity-neoforge/src/main/java/com/example/blockentity/Integration.java
[Fabric block entity example]: ../examples/block-entity-fabric/src/main/java/com/example/blockentity/Integration.java
[card provider]: ../examples/card-neoforge/src/main/java/com/example/card/DiceCardDeviceProvider.java
[card slot tag]: ../examples/card-neoforge/src/main/resources/data/oc2/tags/item/devices/card.json
[DeviceTypes]: ../common/src/main/java/li/cil/oc2/api/bus/device/DeviceTypes.java
[VMDevice]: ../common/src/main/java/li/cil/oc2/api/bus/device/vm/VMDevice.java
[VMContext]: ../common/src/main/java/li/cil/oc2/api/bus/device/vm/context/VMContext.java
[examples]: ../examples
[Sedna]: https://github.com/fnuecke/sedna
