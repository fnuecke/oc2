/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.registries.Registrar;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.block.BlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.*;
import li.cil.oc2.common.bus.device.rpc.block.*;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class Providers {
    public static Registrar<BlockDeviceProvider> blockDeviceProviderRegistry() {
        return ProviderRegistry.BLOCK_DEVICE_PROVIDER_REGISTRY;
    }

    public static Registrar<ItemDeviceProvider> itemDeviceProviderRegistry() {
        return ProviderRegistry.ITEM_DEVICE_PROVIDER_REGISTRY;
    }

    public static Optional<String> optionalKey(@Nullable final BlockDeviceProvider provider) {
        return optionalKey(blockDeviceProviderRegistry(), provider);
    }

    public static Optional<String> optionalKey(@Nullable final ItemDeviceProvider provider) {
        return optionalKey(itemDeviceProviderRegistry(), provider);
    }

    ///////////////////////////////////////////////////////////////////

    private static <T> Optional<String> optionalKey(final Registrar<T> registrar, @Nullable final T value) {
        if (value == null) {
            return Optional.empty();
        }

        final ResourceLocation id = registrar.getId(value);
        return id == null ? Optional.empty() : Optional.of(id.toString());
    }

    ///////////////////////////////////////////////////////////////////

    public static void registerBlockDeviceProviders(final BiConsumer<String, Supplier<BlockDeviceProvider>> registry) {
        registry.accept("block", BlockStateObjectDeviceProvider::new);
        registry.accept("block_entity", BlockEntityObjectDeviceProvider::new);

        registry.accept("block_entity/capability", BlockEntityCapabilityDeviceProvider::new);
        registry.accept("energy_storage", EnergyStorageBlockDeviceProvider::new);
        registry.accept("item_handler", ItemHandlerBlockDeviceProvider::new);

        registerPlatformBlockDeviceProviders(registry);
    }

    @ExpectPlatform
    public static void registerPlatformBlockDeviceProviders(final BiConsumer<String, Supplier<BlockDeviceProvider>> registry) {
        throw new AssertionError();
    }

    /**
     * @see #registerPlatformBlockDeviceProviders(BiConsumer)
     */
    @ExpectPlatform
    public static void registerPlatformItemDeviceProviders(final BiConsumer<String, Supplier<ItemDeviceProvider>> registry) {
        throw new AssertionError();
    }

    public static void registerItemDeviceProviders(final BiConsumer<String, Supplier<ItemDeviceProvider>> registry) {
        registry.accept("memory", MemoryItemDeviceProvider::new);
        registry.accept("hard_drive", HardDriveItemDeviceProvider::new);
        registry.accept("hard_drive_custom", HardDriveWithExternalDataItemDeviceProvider::new);
        registry.accept("flash_memory", FlashMemoryItemDeviceProvider::new);
        registry.accept("flash_memory_custom", FlashMemoryWithExternalDataItemDeviceProvider::new);
        registry.accept("redstone_interface_card", RedstoneInterfaceCardItemDeviceProvider::new);
        registry.accept("network_interface_card", NetworkInterfaceCardItemDeviceProvider::new);
        registry.accept("network_tunnel_card", NetworkTunnelCardItemDeviceProvider::new);
        registry.accept("file_import_export_card", FileImportExportCardItemDeviceProvider::new);
        registry.accept("sound_card", SoundCardItemDeviceProvider::new);

        registry.accept("inventory_operations_module", InventoryOperationsModuleDeviceProvider::new);
        registry.accept("block_operations_module", BlockOperationsModuleDeviceProvider::new);
        registry.accept("network_tunnel_module", NetworkTunnelModuleItemDeviceProvider::new);

        registry.accept("item_stack/capability", ItemStackCapabilityDeviceProvider::new);
        registry.accept("energy_storage", EnergyStorageItemDeviceProvider::new);
        registry.accept("item_handler", ItemHandlerItemDeviceProvider::new);

        registerPlatformItemDeviceProviders(registry);
    }
}
