package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.block.*;
import li.cil.oc2.common.bus.device.provider.item.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public final class Providers {
    private static final DeferredRegister<BlockDeviceProvider> BLOCK_DEVICE_PROVIDERS = DeferredRegister.create(BlockDeviceProvider.class, API.MOD_ID);
    private static final DeferredRegister<ItemDeviceProvider> ITEM_DEVICE_PROVIDERS = DeferredRegister.create(ItemDeviceProvider.class, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final Supplier<IForgeRegistry<BlockDeviceProvider>> BLOCK_DEVICE_PROVIDER_REGISTRY = BLOCK_DEVICE_PROVIDERS.makeRegistry("block_device_providers", RegistryBuilder::new);
    public static final Supplier<IForgeRegistry<ItemDeviceProvider>> ITEM_DEVICE_PROVIDER_REGISTRY = ITEM_DEVICE_PROVIDERS.makeRegistry("item_device_providers", RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        BLOCK_DEVICE_PROVIDERS.register("block", BlockStateDeviceProvider::new);
        BLOCK_DEVICE_PROVIDERS.register("tile_entity", TileEntityDeviceProvider::new);

        BLOCK_DEVICE_PROVIDERS.register("energy_storage", EnergyStorageBlockDeviceProvider::new);
        BLOCK_DEVICE_PROVIDERS.register("fluid_handler", FluidHandlerBlockDeviceProvider::new);
        BLOCK_DEVICE_PROVIDERS.register("item_handler", ItemHandlerBlockDeviceProvider::new);

        BLOCK_DEVICE_PROVIDERS.register("disk_drive", DiskDriveDeviceProvider::new);

        ITEM_DEVICE_PROVIDERS.register("memory", MemoryItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("hard_drive", HardDriveItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("hard_drive_custom", HardDriveWithExternalDataItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("flash_memory", FlashMemoryItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("flash_memory_custom", FlashMemoryWithExternalDataItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("redstone_interface_card", RedstoneInterfaceCardItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("network_interface_card", NetworkInterfaceCardItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("cloud_interface_card", CloudInterfaceCardItemDeviceProvider::new);

        ITEM_DEVICE_PROVIDERS.register("inventory_operations_module", InventoryOperationsModuleDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("block_operations_module", BlockOperationsModuleDeviceProvider::new);

        BLOCK_DEVICE_PROVIDERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEM_DEVICE_PROVIDERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
