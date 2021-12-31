package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.bus.device.util.DeviceTypeImpl;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class DeviceTypes {
    private static final DeferredRegister<DeviceType> DEVICE_TYPES = RegistryUtils.create(DeviceType.class);

    ///////////////////////////////////////////////////////////////////

    public static final Supplier<IForgeRegistry<DeviceType>> DEVICE_TYPE_REGISTRY = DEVICE_TYPES.makeRegistry(DeviceType.REGISTRY.getPath(), RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        register("memory");
        register("hard_drive");
        register("flash_memory");
        register("card");
        register("robot_module");
        register("floppy");
    }

    ///////////////////////////////////////////////////////////////////

    private static void register(final String name) {
        DEVICE_TYPES.register(name, () -> new DeviceTypeImpl(
            new ResourceLocation(API.MOD_ID, "gui/icon/" + name),
            text("gui.{mod}.device_type." + name)
        ));
    }
}
