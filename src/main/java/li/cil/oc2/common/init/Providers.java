package li.cil.oc2.common.init;

import li.cil.oc2.Constants;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.*;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;

public final class Providers {
    public static final SimpleRegistry<BlockDeviceProvider> BLOCK_DEVICE_PROVIDER_REGISTRY = FabricRegistryBuilder.createSimple(BlockDeviceProvider.class, Constants.BLOCK_DEVICE_PROVIDER_REGISTRY_NAME).buildAndRegister();
    public static final SimpleRegistry<ItemDeviceProvider> ITEM_DEVICE_PROVIDER_REGISTRY = FabricRegistryBuilder.createSimple(ItemDeviceProvider.class, Constants.ITEM_DEVICE_PROVIDER_REGISTRY_NAME).buildAndRegister();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        Registry.register(BLOCK_DEVICE_PROVIDER_REGISTRY, "block", new BlockStateDeviceProvider());
        Registry.register(BLOCK_DEVICE_PROVIDER_REGISTRY, "tile_entity", new TileEntityDeviceProvider());

//        Registry.register(BLOCK_DEVICE_PROVIDER_REGISTRY, "energy_storage", new EnergyStorageBlockDeviceProvider());
        Registry.register(BLOCK_DEVICE_PROVIDER_REGISTRY, "fluid_handler", new FluidHandlerBlockDeviceProvider());
        Registry.register(BLOCK_DEVICE_PROVIDER_REGISTRY, "item_handler", new ItemHandlerBlockDeviceProvider());

        Registry.register(ITEM_DEVICE_PROVIDER_REGISTRY, "item_memory", new MemoryItemDeviceProvider());
        Registry.register(ITEM_DEVICE_PROVIDER_REGISTRY, "item_hard_drive", new HardDriveItemDeviceProvider());
    }
}
