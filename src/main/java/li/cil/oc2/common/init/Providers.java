package li.cil.oc2.common.init;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.*;
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

        ITEM_DEVICE_PROVIDERS.register("item_memory", MemoryItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("item_hard_drive", HardDriveItemDeviceProvider::new);
        ITEM_DEVICE_PROVIDERS.register("item_flash_memory", FlashMemoryItemDeviceProvider::new);

        BLOCK_DEVICE_PROVIDERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEM_DEVICE_PROVIDERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
