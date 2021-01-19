package li.cil.oc2.data;

import li.cil.oc2.api.API;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import static li.cil.oc2.common.block.Blocks.*;
import static li.cil.oc2.common.tags.BlockTags.CABLES;
import static li.cil.oc2.common.tags.BlockTags.DEVICES;

public final class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(final DataGenerator generatorIn, @Nullable final ExistingFileHelper existingFileHelper) {
        super(generatorIn, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerTags() {
        getOrCreateBuilder(DEVICES).add(COMPUTER.get(), REDSTONE_INTERFACE.get());
        getOrCreateBuilder(CABLES).add(BUS_CABLE.get());
    }
}
