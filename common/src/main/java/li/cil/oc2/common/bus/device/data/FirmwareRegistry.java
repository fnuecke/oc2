/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class FirmwareRegistry {
    private static final Registrar<Firmware> REGISTRY = RegistryUtils.builder(Registries.FIRMWARE).build();
    private static final DeferredRegister<Firmware> INITIALIZER = RegistryUtils.getInitializerFor(Registries.FIRMWARE);

    ///////////////////////////////////////////////////////////////////

    public static final RegistrySupplier<Firmware> BUILDROOT = INITIALIZER.register("buildroot", BuildrootFirmware::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    @Nullable
    public static ResourceLocation getKey(final Firmware firmware) {
        return REGISTRY.getId(firmware);
    }

    @Nullable
    public static Firmware getValue(final ResourceLocation location) {
        return REGISTRY.get(location);
    }

    public static Stream<Firmware> values() {
        return StreamSupport.stream(REGISTRY.spliterator(), false);
    }
}
