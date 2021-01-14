package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.bus.device.util.DeviceTypeImpl;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public final class DeviceTypes {
    private static final DeferredRegister<DeviceType> DEVICE_TYPES = DeferredRegister.create(DeviceType.class, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final Supplier<IForgeRegistry<DeviceType>> DEVICE_TYPE_REGISTRY = DEVICE_TYPES.makeRegistry(DeviceType.REGISTRY.getPath(), RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        register("memory");
        register("hard_drive");
        register("flash_memory");
        register("card");
        register("robot_module");

        DEVICE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    ///////////////////////////////////////////////////////////////////

    private static void register(final String name) {
        DEVICE_TYPES.register(name, () -> new DeviceTypeImpl(
                new ResourceLocation(API.MOD_ID, "gui/icon/" + name),
                new TranslationTextComponent("gui.oc2.device_type." + name)
        ));
    }
}
