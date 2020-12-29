package li.cil.oc2;

import li.cil.ceres.Ceres;
import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.common.CommonSetup;
import li.cil.oc2.common.init.*;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(API.MOD_ID)
public final class OpenComputers {
    public OpenComputers() {
        Items.initialize();
        Blocks.initialize();
        TileEntities.initialize();
        Containers.initialize();
        Providers.initialize();
        DeviceTypes.initialize();
        BaseBlockDevices.initialize();
        Firmwares.initialize();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(CommonSetup::run);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::run);

        // Do class lookup in a separate thread to avoid blocking for too long.
        // Specifically, this is to run detection of annotated types via the Reflections
        // library in the serialization library and the device tree registry.
        new Thread(() -> {
            Ceres.initialize();
            DeviceTreeRegistry.initialize();
        }).start();
    }
}
