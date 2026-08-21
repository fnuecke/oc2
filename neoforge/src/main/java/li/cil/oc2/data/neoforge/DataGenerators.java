/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber
public final class DataGenerators {
    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        haltWhenDataGenerationFinishes();

        final var generator = event.getGenerator();
        final var output = generator.getPackOutput();
        final var lookupProvider = event.getLookupProvider();
        final var existingFileHelper = event.getExistingFileHelper();

        final var blockTags = generator.addProvider(event.includeServer(),
                new ModBlockTagsProvider(output, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(), new ModLootTableProvider(output, lookupProvider));
        generator.addProvider(event.includeServer(), new ModRecipesProvider(output, lookupProvider));

        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, existingFileHelper));
    }

    private DataGenerators() {
    }

    // ------------------------------------------------------------- //

    private static void haltWhenDataGenerationFinishes() {
        final Thread dataGenerationThread = Thread.currentThread();
        final AtomicBoolean failed = new AtomicBoolean();

        final Thread.UncaughtExceptionHandler previousHandler = dataGenerationThread.getUncaughtExceptionHandler();
        dataGenerationThread.setUncaughtExceptionHandler((thread, error) -> {
            failed.set(true);
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, error);
            }
        });

        final Thread watchdog = new Thread(() -> {
            try {
                dataGenerationThread.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            Runtime.getRuntime().halt(failed.get() ? 1 : 0);
        }, "datagen-exit-watchdog");

        watchdog.setDaemon(true);
        watchdog.start();
    }
}
