package li.cil.oc2.common.util;

import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.event.ForgeEventHandlers;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Utility class for testing async functionality.
 */
public final class AsyncTestUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TEST_TIMEOUT_MS = 5000;
    
    /**
     * Waits for a condition to become true, with a timeout.
     * 
     * @param condition The condition to wait for.
     * @param timeoutMs The maximum time to wait in milliseconds.
     * @return true if the condition became true within the timeout, false otherwise.
     */
    public static boolean waitForCondition(BooleanSupplier condition, long timeoutMs) {
        final long startTime = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                return false;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }
    
    /**
     * Verifies that async operations are working correctly.
     * 
     * @return A future that completes with a test UUID when verification is done.
     */
    public static CompletableFuture<UUID> verifyAsyncOperations() {
        if (!AsyncConfig.SERVER.asyncStorageOperations.get()) {
            return CompletableFuture.completedFuture(null);
        }
        
        LOGGER.info("Verifying async operations...");
        
        // Test basic async execution
        return CompletableFuture.supplyAsync(() -> {
            if (AsyncConfig.SERVER.enableSuperDebug.get()) {
                LOGGER.debug("Async test operation running on thread: {}", Thread.currentThread().getName());
            }
            
            // Add a small delay to ensure async behavior
            try {
                Thread.sleep(100);
                return null; // Return value from the supplier
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Async test interrupted", e);
            }
        }, AsyncUtils.getAsyncExecutor())
        .thenCompose(v -> {
            // Verify we can switch back to server thread
            return AsyncUtils.onServerThread(() -> {
                MinecraftServer server = ForgeEventHandlers.getCurrentServer();
                if (server == null) {
                    throw new IllegalStateException("Server not available during async test");
                }
                
                if (AsyncConfig.SERVER.enableSuperDebug.get()) {
                    LOGGER.debug("Successfully switched back to server thread");
                }
                
                // Generate a test UUID for storage testing
                UUID testId = UUID.randomUUID();
                if (AsyncConfig.SERVER.enableSuperDebug.get()) {
                    LOGGER.debug("Generated test UUID: {}", testId);
                }
                
                return testId;
            });
        })
        .whenComplete((result, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Async test failed", throwable);
            } else {
                LOGGER.info("Async operations verified successfully");
            }
        });
    }
}
