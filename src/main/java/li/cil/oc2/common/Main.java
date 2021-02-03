package li.cil.oc2.common;

import li.cil.ceres.Ceres;
import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.serialization.serializers.Serializers;
import li.cil.oc2.common.tags.BlockTags;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.tileentity.TileEntities;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.sedna.Sedna;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(API.MOD_ID)
public final class Main {
    public Main() {
        Ceres.initialize();
        Sedna.initialize();
        Serializers.initialize();

        Config.initialize();

        ItemTags.initialize();
        BlockTags.initialize();
        Items.initialize();
        Blocks.initialize();
        TileEntities.initialize();
        Entities.initialize();
        Containers.initialize();
        SoundEvents.initialize();

        Providers.initialize();
        DeviceTypes.initialize();
        BlockDeviceDataRegistration.initialize();
        Firmwares.initialize();

        FMLJavaModLoadingContext.get().getModEventBus().register(CommonSetup.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().register(ClientSetup.class));
    }
}
