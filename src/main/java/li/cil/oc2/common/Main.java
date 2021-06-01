package li.cil.oc2.common;

import li.cil.ceres.Ceres;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import li.cil.oc2.common.serialization.serializers.Serializers;
import li.cil.oc2.common.tags.BlockTags;
import li.cil.oc2.common.tileentity.TileEntities;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.sedna.Sedna;
import net.fabricmc.api.ModInitializer;

public final class Main implements ModInitializer {
    @Override
    public void onInitialize() {
        Ceres.initialize();
        Sedna.initialize();
        Serializers.initialize();
        BlockTags.initialize();
        Items.initialize();
        Blocks.initialize();
        TileEntities.initialize();
        Containers.initialize();
        RecipeSerializers.initialize();
        SoundEvents.initialize();

        Providers.initialize();
        DeviceTypes.initialize();
        BlockDeviceDataRegistration.initialize();
        Firmwares.initialize();
    }
}
