package li.cil.oc2.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        final DataGenerator generator = event.getGenerator();

        if (event.includeClient()) {
            final ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
            generator.addProvider(new BlockStates(generator, existingFileHelper));
            generator.addProvider(new ItemModels(generator, existingFileHelper));
            generator.addProvider(new LootTables(generator));
            generator.addProvider(new CraftingRecipes(generator));
        }
    }
}
