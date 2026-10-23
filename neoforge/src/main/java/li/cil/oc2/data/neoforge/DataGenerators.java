/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber
public final class DataGenerators {
    private static final AtomicBoolean EXIT_WATCHDOG_INSTALLED = new AtomicBoolean();

    @SubscribeEvent
    public static void gatherServerData(final GatherDataEvent.Server event) {
        haltWhenDataGenerationFinishes();

        event.createBlockAndItemTags(ModBlockTagsProvider::new, ModItemTagsProvider::new);
        event.createProvider(ModLootTableProvider::new);
        event.createProvider(ModRecipesProvider::new);
    }

    @SubscribeEvent
    public static void gatherClientData(final GatherDataEvent.Client event) {
        haltWhenDataGenerationFinishes();

        event.createProvider(ModModelProvider::new);
        event.createProvider(ModManualProvider::new);
    }

    private DataGenerators() {
    }

    // --------------------------------------------------------------------- //

    private static void haltWhenDataGenerationFinishes() {
        if (!EXIT_WATCHDOG_INSTALLED.compareAndSet(false, true)) {
            return;
        }

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
