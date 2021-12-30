package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class BlockDeviceDataRegistration {
    private static final DeferredRegister<BlockDeviceData> INITIALIZER = RegistryUtils.create(BlockDeviceData.class);

    ///////////////////////////////////////////////////////////////////

    public static final Supplier<IForgeRegistry<BlockDeviceData>> REGISTRY = INITIALIZER.makeRegistry(BlockDeviceData.REGISTRY.getPath(), RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<BlockDeviceData> BUILDROOT = INITIALIZER.register("buildroot", BuildrootBlockDeviceData::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }
}
