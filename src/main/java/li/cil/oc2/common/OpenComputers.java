package li.cil.oc2.common;

import li.cil.ceres.Ceres;
import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BaseBlockDevices;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.serialization.serializers.Serializers;
import li.cil.oc2.common.tileentity.TileEntities;
import li.cil.sedna.Sedna;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(API.MOD_ID)
public final class OpenComputers {
    public OpenComputers() {
        Ceres.initialize();
        Sedna.initialize();
        Serializers.initialize();
        Config.initialize();
        Items.initialize();
        Blocks.initialize();
        TileEntities.initialize();
        Containers.initialize();
        Providers.initialize();
        DeviceTypes.initialize();
        BaseBlockDevices.initialize();
        Firmwares.initialize();

        FMLJavaModLoadingContext.get().getModEventBus().register(CommonSetup.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(ClientSetup.class);
    }
}
