/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.data.event.GatherDataEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        final DataGenerator generator = event.getGenerator();
        final ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

            generator.addProvider(event.includeServer(), new ModLootTableProvider(generator));
            final BlockTagsProvider blockTagProvider = new ModBlockTagsProvider(generator, existingFileHelper);
            generator.addProvider(event.includeServer(), blockTagProvider);
            generator.addProvider(event.includeServer(), new ModItemTagsProvider(generator, blockTagProvider, existingFileHelper));
            generator.addProvider(event.includeServer(), new ModRecipesProvider(generator));

            generator.addProvider(event.includeClient(), new ModBlockStateProvider(generator, existingFileHelper));
            generator.addProvider(event.includeClient(), new ModItemModelProvider(generator, existingFileHelper));
    }
}
