/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Optional;

public final class ProviderRegistry {
    public static final Registrar<BlockDeviceProvider> BLOCK_DEVICE_PROVIDER_REGISTRY =
        RegistryUtils.builder(Registries.BLOCK_DEVICE_PROVIDER).build();
    private static final DeferredRegister<BlockDeviceProvider> BLOCK_DEVICE_PROVIDERS =
        RegistryUtils.getInitializerFor(Registries.BLOCK_DEVICE_PROVIDER);

    ///////////////////////////////////////////////////////////////////

    public static final Registrar<ItemDeviceProvider> ITEM_DEVICE_PROVIDER_REGISTRY =
        RegistryUtils.builder(Registries.ITEM_DEVICE_PROVIDER).build();
    private static final DeferredRegister<ItemDeviceProvider> ITEM_DEVICE_PROVIDERS =
        RegistryUtils.getInitializerFor(Registries.ITEM_DEVICE_PROVIDER);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        Providers.registerBlockDeviceProviders(BLOCK_DEVICE_PROVIDERS::register);
        Providers.registerItemDeviceProviders(ITEM_DEVICE_PROVIDERS::register);
    }

    public static Optional<String> optionalKey(@Nullable final BlockDeviceProvider provider) {
        return optionalKey(BLOCK_DEVICE_PROVIDER_REGISTRY, provider);
    }

    public static Optional<String> optionalKey(@Nullable final ItemDeviceProvider provider) {
        return optionalKey(ITEM_DEVICE_PROVIDER_REGISTRY, provider);
    }

    ///////////////////////////////////////////////////////////////////

    private static <T> Optional<String> optionalKey(final Registrar<T> registrar, @Nullable final T value) {
        if (value == null) {
            return Optional.empty();
        }

        final ResourceLocation id = registrar.getId(value);
        return id == null ? Optional.empty() : Optional.of(id.toString());
    }
}
