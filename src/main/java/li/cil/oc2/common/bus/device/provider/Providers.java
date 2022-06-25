/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.block.BlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.*;
import li.cil.oc2.common.bus.device.rpc.block.*;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class Providers {
    public static IForgeRegistry<BlockDeviceProvider> blockDeviceProviderRegistry() {
        return ProviderRegistry.BLOCK_DEVICE_PROVIDER_REGISTRY.get();
    }

    public static IForgeRegistry<ItemDeviceProvider> itemDeviceProviderRegistry() {
        return ProviderRegistry.ITEM_DEVICE_PROVIDER_REGISTRY.get();
    }

    public static void registerBlockDeviceProviders(final BiConsumer<String, Supplier<BlockDeviceProvider>> registry) {
        registry.accept("block", BlockStateObjectDeviceProvider::new);
        registry.accept("block_entity", BlockEntityObjectDeviceProvider::new);

        registry.accept("block_entity/capability", BlockEntityCapabilityDeviceProvider::new);
        registry.accept("energy_storage", EnergyStorageBlockDeviceProvider::new);
        registry.accept("fluid_handler", FluidHandlerBlockDeviceProvider::new);
        registry.accept("item_handler", ItemHandlerBlockDeviceProvider::new);
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
        registry.accept("internet_card", InternetCardItemDeviceProvider::new);
        registry.accept("file_import_export_card", FileImportExportCardItemDeviceProvider::new);
        registry.accept("sound_card", SoundCardItemDeviceProvider::new);

        registry.accept("inventory_operations_module", InventoryOperationsModuleDeviceProvider::new);
        registry.accept("block_operations_module", BlockOperationsModuleDeviceProvider::new);
        registry.accept("network_tunnel_module", NetworkTunnelModuleItemDeviceProvider::new);

        registry.accept("item_stack/capability", ItemStackCapabilityDeviceProvider::new);
        registry.accept("energy_storage", EnergyStorageItemDeviceProvider::new);
        registry.accept("fluid_handler", FluidHandlerItemDeviceProvider::new);
        registry.accept("item_handler", ItemHandlerItemDeviceProvider::new);
    }
}
