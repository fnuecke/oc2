package li.cil.oc2.common.init;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.provider.DeviceProvider;
import li.cil.oc2.common.bus.device.provider.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public final class Providers {
    private static final DeferredRegister<DeviceProvider> PROVIDERS = DeferredRegister.create(DeviceProvider.class, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final Supplier<IForgeRegistry<DeviceProvider>> PROVIDERS_REGISTRY = PROVIDERS.makeRegistry("device_providers", RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        PROVIDERS.register("block", BlockDeviceProvider::new);
        PROVIDERS.register("tile_entity", TileEntityDeviceProvider::new);

        PROVIDERS.register("energy_storage", EnergyStorageDeviceProvider::new);
        PROVIDERS.register("fluid_handler", FluidHandlerDeviceProvider::new);
        PROVIDERS.register("item_handler", ItemHandlerDeviceProvider::new);

        PROVIDERS.register("item_memory", MemoryItemDeviceProvider::new);
        PROVIDERS.register("item_hard_drive", HardDriveItemDeviceProvider::new);

        PROVIDERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
