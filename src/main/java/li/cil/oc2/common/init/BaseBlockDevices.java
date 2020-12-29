package li.cil.oc2.common.init;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.BaseBlockDevice;
import li.cil.oc2.common.bus.device.data.BuildrootRootFileSystem;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public final class BaseBlockDevices {
    private static final DeferredRegister<BaseBlockDevice> INITIALIZER = DeferredRegister.create(BaseBlockDevice.class, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final Supplier<IForgeRegistry<BaseBlockDevice>> REGISTRY = INITIALIZER.makeRegistry(BaseBlockDevice.REGISTRY.getPath(), RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<BaseBlockDevice> BUILDROOT = INITIALIZER.register("buildroot", BuildrootRootFileSystem::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        INITIALIZER.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
