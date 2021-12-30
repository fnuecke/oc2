package li.cil.oc2.data;

import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        final DataGenerator generator = event.getGenerator();
        final ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        if (event.includeServer()) {
            generator.addProvider(new ModLootTableProvider(generator));
            final BlockTagsProvider blockTagProvider = new ModBlockTagsProvider(generator, existingFileHelper);
            generator.addProvider(blockTagProvider);
            generator.addProvider(new ModItemTagsProvider(generator, blockTagProvider, existingFileHelper));
            generator.addProvider(new ModRecipesProvider(generator));
        }
        if (event.includeClient()) {
            generator.addProvider(new ModBlockStateProvider(generator, existingFileHelper));
            generator.addProvider(new ModItemModelProvider(generator, existingFileHelper));
        }
    }
}
