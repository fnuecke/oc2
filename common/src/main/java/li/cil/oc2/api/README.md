# The OpenComputers II API

Welcome to the API of `oc2`, fellow developer! This document will hopefully provide a sufficient overview of what
integrations this API allows, and how to best implement them. The primary purpose of the API is to allow other mods to
implement their own devices, to be used by the computers in this mod.

## Loaders and Packages

`oc2` is built for both Fabric and NeoForge. The API is split accordingly:

- [`li.cil.oc2.api`](.) is loader-independent. Devices, providers, registry keys and the annotations live here.
- [`li.cil.oc2.api.fabric`](../../../../../../../../fabric/src/main/java/li/cil/oc2/api/fabric) holds the Fabric
  `BlockApiLookup`/`ItemApiLookup`/`EntityApiLookup` instances.
- [`li.cil.oc2.api.neoforge`](../../../../../../../../neoforge/src/main/java/li/cil/oc2/api/neoforge) holds the matching
  NeoForge `BlockCapability`/`ItemCapability`/`EntityCapability` instances.

The two platform packages mirror each other: every entry in `Lookups` has a counterpart under the same name in
`Capabilities`, and both use the same resource locations.

## The `RPCDevice`

The core of the `RPCDevice` system is the [`RPCDevice`](bus/device/rpc/RPCDevice.java) interface itself. It defines a
list of [`RPCMethods`](bus/device/rpc/RPCMethod.java), which represent the methods that can be called on the device.
This is the suggested type of device to add, and allows easily exposing methods in your classes to virtual machines.

> [!NOTE]
> If you've seen the APIs of OpenComputers or ComputerCraft before, this should feel fairly familiar.

### Threading

Methods run on the main server thread by default. Both ways of declaring a method, `Callback.synchronize()` and the
`AbstractRPCMethod` constructors that omit the flag, default to synchronizing. This what you want if your logic does
anything with the level or stuff in it. Opting out with `@Callback(synchronize = false)` makes calls much cheaper,
since a synchronized call costs at least one tick, but is only safe for methods that touch nothing but their own
state. Do **not** try to synchronize against the server thread here. VMs are joined to the server thread each tick,
so you'd just introduce a deadlock.

### The `ObjectDevice`

It is perfectly fine to implement these interfaces manually. There is a more convenient way, however, when adding a
device explicitly for this mod, in the form of the [`ObjectDevice`](bus/device/object/ObjectDevice.java). This class
allows wrapping a Java object as an `RPCDevice`. Methods to be exposed in the device are defined by adding the
[`Callback`](bus/device/object/Callback.java) annotation to methods of the object's class.

### Type Names

In addition to methods, `RPCDevices` provide a list of "type names". These names are meta-data, that can be used by
programs running in the virtual machines to identify devices. These should be clear and unique, to avoid confusion
between device types. For example, "machine" would probably be a little too generic, whereas "redstone_furnace" would
probably be a little better. Note that for all `BlockEntities` providing devices, the registry name of their
`BlockEntityType` is automatically added to the list of type names; for blocks without a block entity, the block's
registry name is used instead. Equally, for all `Items` providing devices, their registry name is automatically added
to the list of type names.

### Method Name Collisions

All `RPCDevices` found for a particular `BlockEntity` or `Item` will be merged, and present as one singular `RPCDevice`
to the virtual machine. This means that not only type names are merged, but `RPCMethodGroups` are merged into a single
list as well. In most cases, it is fine to return each `RPCMethod` as its own `RPCMethodGroup`. For this reason, the
`RPCMethod` interface extends the `RPCMethodGroup` interface.

The system supports method overloading to some degree. `RPCMethodGroups` with a matching method name are queried one
by one for an overload matching the invocation's parameters. `RPCMethods` provide a default implementation for this,
using the declared parameter types to determine if they match.

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
[`RPCTypeAdapter`](bus/device/rpc/RPCTypeAdapter.java) record. Register one with the
`Registries.RPC_TYPE_ADAPTER` registry, exactly as you would a device provider. `ItemStack` and
`Direction` are registered this way already.

Adapters apply in registration order, so registering one for a type the mod already covers replaces it. An adapter may
implement Gson's `JsonSerializer`, `JsonDeserializer` or both; only implement the direction you need. The built-in
`ItemStack` adapter serializes only, because stacks are handed to the guest but never accepted from it.

### Device Lifecycle

Where needed, the optional interface methods `mount()`, `unmount()` and `dispose()` may be implemented, to react to
device lifecycle events. This can be useful in case some state needs to be initialized or reset, when the computer
starts or stops, or the device is connected to or disconnected from a computer.

These methods are called in the following cases:

- `mount()` is called when a device is added to a running computer, or the computer it was added to starts running. It
  is also called when a computer resumes running after the chunk it sits in is loaded.
- `unmount()` is called when the computer suspends because its chunk is unloaded or the server stops, when the computer
  stops, and when the device is removed from a running computer. If `mount()` was called, `unmount()` is guaranteed to
  follow.
- `dispose()` is called when the computer stops or the device is removed, after `unmount()`. It is not a terminal
  state: a device still on the bus when its computer starts again is mounted again, so it must leave itself usable.
  Releasing resources here is fine as long as they can be re-acquired on the next mount.

Note that suspending and stopping are not distinguished. Both end in `unmount()`; only stopping continues on to
`dispose()`.

This can be useful for various things. For example:

- Setting a flag in the block the device is associated with.
    - Set the flag in `mount()`.
    - Unset the flag in `unmount()`.
- Track out-of-minecraft resources, such as a file with extra data.
    - Create and open the file in `mount()`.
    - Close the file in `unmount()`.
    - Delete the file in `dispose()`, and be ready to create it again on the next `mount()`.

### No Active Back-channel

Unlike some other computer mods (e.g. OpenComputers and ComputerCraft), there is no *active* back-channel in the
`RPCDevice` API. In other words, it is not possible for `RPCDevices` to raise events in the virtual machines. The only
way to provide data to the virtual machines is as values returned from exposed methods. Programs running in the virtual
machines will always have to poll for changed data.

> [!NOTE]
> Guest programs do have an event API, and the mod itself uses it to signal completed robot actions. That path is
> internal; there is no way to reach it from a `Device`.

## The `BlockDeviceProvider` and `ItemDeviceProvider`

So let's say you have some `RPCDevice` at hand (or a `VMDevice`). Now you want the computer to use it. The core
functionality that makes `Devices` available to the mod are the
[`BlockDeviceProvider`](bus/device/provider/BlockDeviceProvider.java) and the
[`ItemDeviceProvider`](bus/device/provider/ItemDeviceProvider.java) interfaces.

There exists a registry for each, with which all block and item providers must be registered. These registries are
queried to collect devices for a given block in the world, or an item in a machine inventory.

The two interfaces differ in return type, deliberately. `BlockDeviceProvider` returns an
[`Invalidatable<Device>`](util/Invalidatable.java): a block can drop its device out of band, and invalidating the value
makes the bus drop it and rescan. `ItemDeviceProvider` returns a plain `Optional<ItemDevice>`, because an item device is
owned by the slot holding its stack and is re-queried whenever that slot changes, so there is nothing to invalidate.

Both also carry a `disposeMissing(query, tag)` hook. Despite living next to the device lifecycle, this is not
`unmount()`: it is a last-resort cleanup for a device that vanished while its computer was unloaded, handing you the
tag it last serialized. Implement it only if the device owns state outside that tag.

### Registering Providers

[`Registries`](util/Registries.java) holds the `ResourceKey` of every registry the mod creates. Register with them the
way your loader registers with any other registry.

On NeoForge, a `DeferredRegister` on the mod event bus is enough:

```java
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.util.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class Providers {
    static final DeferredRegister<BlockDeviceProvider> BLOCK_DEVICE_PROVIDERS =
        DeferredRegister.create(Registries.BLOCK_DEVICE_PROVIDER, "my_mod_id");

    // Called from mod construction, if oc2 is present.
    static void initialize(final IEventBus modEventBus) {
        BLOCK_DEVICE_PROVIDERS.register("my_calculator_device", ModDeviceProvider::new);
        BLOCK_DEVICE_PROVIDERS.register(modEventBus);
    }
}
```

On Fabric the registries are created by `oc2` itself, so they only exist once `oc2` has initialized. Registering from a
plain `ModInitializer` is a race. Use the `oc2:registration` entrypoint instead, which is invoked after every registry
the mod creates exists, and look the registry up by its key:

```java
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.platform.FabricRegistrationInitializer;
import li.cil.oc2.api.util.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class ModRegistration implements FabricRegistrationInitializer {
    @Override
    public void registerObjects() {
        Registry.register(registry(Registries.BLOCK_DEVICE_PROVIDER),
            ResourceLocation.fromNamespaceAndPath("my_mod_id", "my_calculator_device"),
            new ModDeviceProvider());
    }

    @SuppressWarnings("unchecked")
    private static <T> Registry<T> registry(final ResourceKey<Registry<T>> key) {
        return (Registry<T>) BuiltInRegistries.REGISTRY.get(key.location());
    }
}
```

Declare the entrypoint in your `fabric.mod.json`:

```json
{
    "entrypoints": {
        "oc2:registration": [
            "com.example.ModRegistration"
        ]
    }
}
```

### Block Devices

Blocks devices are queried for all blocks adjacent to a `Bus Interface` that is connected to some computer via some
`Bus Cable` and another `Bus Interface`. Connected `Bus Cables` with attached `Bus Interfaces` define a
[`DeviceBus`](bus/DeviceBus.java). Computers collect all devices attached to the `DeviceBus` and make them available to
the virtual machine they run. Each registered `BlockDeviceProvider` is queried for a block in question, and the found
`RPCDevices` are aggregated into one `RPCDevice` proxy.

> [!NOTE]
> `BusInterfaces` look for `Devices` using `BlockDeviceProviders`.

The mod comes with a set of convenience `BlockDeviceProviders`, which enable offering devices in various ways. This
means you don't necessarily have to implement your own provider. The following built-in providers exist:

- `BlockEntities` are queried for the device capability (`Capabilities.Device.BLOCK` on NeoForge,
  `Lookups.Device.BLOCK` on Fabric). If there is one, the returned device is used.
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

[`DeviceTypes`](bus/device/DeviceTypes.java) lists the built-in slot types as `Supplier<DeviceType>`, resolved from the
registry on first use: `DeviceTypes.CARD.get()`. Calling `get()` before the mod has registered its device types throws,
so do not resolve them from a static initializer of your own.

## Other Capabilities

Besides devices, the mod looks for and provides a handful of other capabilities. Each has a Fabric lookup and a NeoForge
capability under the same name:

| Purpose                                   | Fabric                         | NeoForge                            |
|-------------------------------------------|--------------------------------|-------------------------------------|
| Expose a block/item as a device           | `Lookups.Device`               | `Capabilities.Device`               |
| Join a device bus, e.g. a custom cable    | `Lookups.DeviceBusElement`     | `Capabilities.DeviceBusElement`     |
| Emit a redstone signal for a device       | `Lookups.RedstoneEmitter`      | `Capabilities.RedstoneEmitter`      |
| Exchange ethernet frames with connectors  | `Lookups.NetworkInterface`     | `Capabilities.NetworkInterface`     |
| Let installed modules reach their host    | `Lookups.Robot`                | `Capabilities.Robot`                |

Fabric has no built-in entity lookups for energy or item storage, which robots need. We declare our own:
`EnergyStorage.ENTITY` and `ItemStorage.ENTITY` in `li.cil.oc2.api.fabric`; the same classes re-export the standard
`SIDED` and `ITEM` lookups so all three can be reached from one place. On NeoForge the corresponding NeoForge
capabilities already cover entities, and are used directly.

## The `VMDevice`

`VMDevices` are low-level, memory-mapped devices, emulating "real" hardware, and thus requiring driver support by the
operating system running in the virtual machines.

> [!NOTE]
> `VMDevices` are very low-level, and something most people can ignore.

The core of the `VMDevice` system is the [`VMDevice`](bus/device/vm/VMDevice.java) interface itself. It defines a proxy
used to load and unload actual emulated hardware. `VMDevices` use the
[`VMContext`](bus/device/vm/context/VMContext.java) to bind hardware to the virtual machine upon initialization. This
typically includes reserving an address block in memory, possibly hooking up interrupts and reserving host memory from
the memory tracker. In most cases, `VMDevices` will add a `MemoryMappedDevice` to the `MemoryMap`, an interface used
by [Sedna], the VM implementation used to run the computers in this mod.

On the off chance you wish to add a `VMDevice`, and the existing devices do not suffice for reference, open a discussion
on Github. I'll skip more details here, since I doubt most people would care, and it might instead scare people off...

## Examples

These examples are roughly sorted in order of likely usefulness. Most mods will want to maintain a optional integration
with this mod, instead of a hard dependency, so these examples are shown first.

### Device for own `BlockEntity`

In this example, a device is made available for a custom `BlockEntity`. The device itself is loader-independent:

```java
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;

final class Integration {
    static RPCDevice createDevice(final ModBlockEntity blockEntity) {
        return new ObjectDevice(new ModBlockEntityDevice(blockEntity), "mod_block_entity");
    }

    // Note: this being a record is relevant, as it implements equals() for us. When manually implementing devices,
    // overriding equals() is strongly recommended, to allow newly picked up devices to be matched to previously
    // existing devices. Otherwise, the devices will technically be removed and re-added every time the device bus
    // scans for device changes. This is particularly relevant when using the lifecycle methods mount(), unmount()
    // and dispose() (e.g. if we were to implement LifecycleAwareDevice on this record).
    record ModBlockEntityDevice(ModBlockEntity blockEntity) {
        @Callback
        public int getMagicValue() {
            return blockEntity.getMagicValue();
        }
    }
}
```

Registering devices is loader specific. On NeoForge, register the capability for your `BlockEntityType`:

```java
import li.cil.oc2.api.neoforge.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// Called from RegisterCapabilitiesEvent, if oc2 is present.

static void registerCapabilities(final RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(Capabilities.Device.BLOCK, ModBlockEntities.MOD_BLOCK_ENTITY.get(),
        (blockEntity, side) -> Integration.createDevice(blockEntity));
}
```

On Fabric, register with the matching lookup:

```java
import li.cil.oc2.api.fabric.Lookups;

// Called from your ModInitializer, if oc2 is present.

static void registerLookups() {
    Lookups.Device.BLOCK.registerForBlockEntity(
        (blockEntity, side) -> Integration.createDevice(blockEntity),
        ModBlockEntities.MOD_BLOCK_ENTITY);
}
```

Both are soft dependencies: gate the call behind a check whether `oc2` is loaded, and nothing in `ModBlockEntity` itself
refers to this API.

Alternatively, annotate methods on the `BlockEntity` directly. This is by far the least code, at the cost of a hard
dependency:

```java
import li.cil.oc2.api.bus.device.object.Callback;
import net.minecraft.world.level.block.entity.BlockEntity;

class ModBlockEntity extends BlockEntity {
    @Callback
    public int getMagicValue() {
        // ...
    }
}
```

### Device for a Third-Party `BlockEntity`

In this example, a simple device providing a single method, `squareRoot`, is made available for the
`FurnaceBlockEntity`. As long as the registration of the `BlockDeviceProvider` is gated behind a check, whether `oc2` is
present, this is a soft dependency.

Using `ObjectDevice`:

```java
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

final class MyCalculatorDevice {
    @Callback(synchronize = false)
    public double squareRoot(final int value) {
        if (value < 0) throw new IllegalArgumentException("Invalid input value!");
        return Math.sqrt(value);
    }
}

public final class ModDeviceProvider implements BlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        // Note: optionally check other conditions, such as settings, on whether to just return empty().
        final BlockEntity blockEntity = query.getLevel().getBlockEntity(query.getQueryPosition());
        if (blockEntity instanceof FurnaceBlockEntity) {
            return Invalidatable.of(new ObjectDevice(new MyCalculatorDevice(), "my_calculator_device"));
        } else {
            return Invalidatable.empty();
        }
    }
}
```

And again, using the `RPCDevice` and `RPCMethod` interfaces directly, if you would rather not depend on the annotation:

```java
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCInvocation;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;

import java.util.Collections;
import java.util.List;

final class ModDevice implements RPCDevice {
    @Override
    public List<String> getTypeNames() {
        return Collections.singletonList("my_calculator_device");
    }

    @Override
    public List<RPCMethodGroup> getMethodGroups() {
        return Collections.singletonList(new RPCMethod() {
            @Override
            public String getName() {
                return "squareRoot";
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }

            @Override
            public Class<?> getReturnType() {
                return double.class;
            }

            @Override
            public RPCParameter[] getParameters() {
                return new RPCParameter[]{() -> int.class};
            }

            @Override
            public Object invoke(final RPCInvocation invocation) {
                final int arg = invocation.getParameters().get(0).getAsInt();
                if (arg < 0) throw new IllegalArgumentException("Invalid input value!");
                return Math.sqrt(arg);
            }
        });
    }
}
```

[Sedna]: https://github.com/fnuecke/sedna
