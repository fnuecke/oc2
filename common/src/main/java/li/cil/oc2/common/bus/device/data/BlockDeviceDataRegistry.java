/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class BlockDeviceDataRegistry {
    private static final Registrar<BlockDeviceData> REGISTRY = RegistryUtils.builder(Registries.BLOCK_DEVICE_DATA).build();
    private static final DeferredRegister<BlockDeviceData> INITIALIZER = RegistryUtils.getInitializerFor(Registries.BLOCK_DEVICE_DATA);

    // ------------------------------------------------------------- //

    public static final RegistrySupplier<BlockDeviceData> BUILDROOT = INITIALIZER.register("buildroot", BuildrootBlockDeviceData::new);

    // ------------------------------------------------------------- //

    public static void initialize() {
    }

    @Nullable
    public static ResourceLocation getKey(final BlockDeviceData data) {
        final ResourceLocation id = REGISTRY.getId(data);
        if (id != null) {
            return id;
        }

        if (data instanceof final ResourceBlockDeviceData resourceData) {
            return resourceData.getLocation();
        }

        return null;
    }

    @Nullable
    public static BlockDeviceData getValue(final ResourceLocation location) {
        final BlockDeviceData value = REGISTRY.get(location);
        if (value != null) {
            return value;
        }
        return FileSystems.getBlockData().get(location);
    }

    public static Stream<BlockDeviceData> values() {
        return Stream.concat(
            StreamSupport.stream(REGISTRY.spliterator(), false),
            FileSystems.getBlockData().values().stream());
    }
}
