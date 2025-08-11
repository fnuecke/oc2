package li.cil.oc2.common.event;

import li.cil.oc2.api.API;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.util.AsyncTestUtils;
import li.cil.oc2.common.util.AsyncUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * Handles Forge lifecycle events to ensure proper initialization and cleanup of async operations.
 */
@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEventHandlers {
    private static final Logger LOGGER = LogManager.getLogger();
    private static MinecraftServer server;

    /**
     * Get the current Minecraft server instance.
     *
     * @return The current Minecraft server instance, or null if not available.
     */
    @Nullable
    public static MinecraftServer getCurrentServer() {
        return server;
    }

    @SubscribeEvent
    public static void handleServerAboutToStart(final ServerAboutToStartEvent event) {
        server = event.getServer();
        LOGGER.info("Server starting, initializing async components");

        // Run async tests if enabled
        if (AsyncConfig.SERVER.runAsyncTests.get()) {
            LOGGER.info("Running async operation tests...");
            AsyncTestUtils.verifyAsyncOperations()
                .thenAccept(uuid -> {
                    if (uuid != null) {
                        LOGGER.debug("Async test completed with UUID: {}", uuid);
                    } else {
                        LOGGER.debug("Async test completed");
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("Async test failed", e);
                    return null;
                });
        }
    }

    @SubscribeEvent
    public static void handleServerStopped(final ServerStoppedEvent event) {
        LOGGER.info("Server stopped, cleaning up async components");
        try {
            AsyncUtils.shutdown();
        } catch (final Exception e) {
            LOGGER.error("Error during async component shutdown", e);
        } finally {
            server = null;
        }
    }
}
