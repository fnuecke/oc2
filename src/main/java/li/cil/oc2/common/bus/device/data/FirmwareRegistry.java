/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class FirmwareRegistry {
    private static final DeferredRegister<Firmware> INITIALIZER = RegistryUtils.getInitializerFor(Registries.FIRMWARE);

    ///////////////////////////////////////////////////////////////////

    private static final Supplier<IForgeRegistry<Firmware>> REGISTRY = INITIALIZER.makeRegistry(Firmware.class, RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<Firmware> BUILDROOT = INITIALIZER.register("buildroot", BuildrootFirmware::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    @Nullable
    public static ResourceLocation getKey(final Firmware firmware) {
        return firmware.getRegistryName();
    }

    @Nullable
    public static Firmware getValue(final ResourceLocation location) {
        return REGISTRY.get().getValue(location);
    }

    public static Stream<Firmware> values() {
        return REGISTRY.get().getValues().stream();
    }
}
