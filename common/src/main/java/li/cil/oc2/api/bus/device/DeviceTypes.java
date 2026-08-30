/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.API;
import li.cil.oc2.api.util.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Lists built-in device types for convenience.
 */
public final class DeviceTypes {
    public static final Supplier<DeviceType> CPU = of("cpu");
    public static final Supplier<DeviceType> MEMORY = of("memory");
    public static final Supplier<DeviceType> HARD_DRIVE = of("hard_drive");
    public static final Supplier<DeviceType> FLASH_MEMORY = of("flash_memory");
    public static final Supplier<DeviceType> CARD = of("card");
    public static final Supplier<DeviceType> ROBOT_MODULE = of("robot_module");
    public static final Supplier<DeviceType> FLOPPY = of("floppy");
    public static final Supplier<DeviceType> NETWORK_TUNNEL = of("network_tunnel");

    // --------------------------------------------------------------------- //

    private static Supplier<DeviceType> of(final String name) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, name);
        return new Supplier<>() {
            private DeviceType value;

            @Override
            public DeviceType get() {
                if (value == null) {
                    value = registry().get(id);
                    if (value == null) {
                        throw new IllegalStateException("Device type [" + id + "] is not registered yet.");
                    }
                }
                return value;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Registry<DeviceType> registry() {
        final Registry<?> registry = BuiltInRegistries.REGISTRY.get(Registries.DEVICE_TYPE.location());
        if (registry == null) {
            throw new IllegalStateException("Registry [" + Registries.DEVICE_TYPE.location() + "] does not exist yet.");
        }
        return (Registry<DeviceType>) registry;
    }

    private DeviceTypes() {
    }
}
