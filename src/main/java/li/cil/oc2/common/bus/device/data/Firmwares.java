package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.Firmware;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public final class Firmwares {
    private static final DeferredRegister<Firmware> INITIALIZER = DeferredRegister.create(Firmware.class, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final Supplier<IForgeRegistry<Firmware>> REGISTRY = INITIALIZER.makeRegistry(Firmware.REGISTRY.getPath(), RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<Firmware> BUILDROOT = INITIALIZER.register("buildroot", BuildrootFirmware::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        INITIALIZER.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
