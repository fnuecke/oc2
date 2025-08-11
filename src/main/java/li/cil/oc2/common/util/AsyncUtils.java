package li.cil.oc2.common.util;

import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.event.ForgeEventHandlers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for handling asynchronous operations with proper error handling and debugging.
 */
public final class AsyncUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Use a dedicated executor for async operations to avoid blocking the main server thread
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newWorkStealingPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() / 2)
    );
    
    /**
     * Gets the async executor service.
     * 
     * @return The async executor service.
     */
    public static ExecutorService getAsyncExecutor() {
        return ASYNC_EXECUTOR;
    }

    // Prevent instantiation
    private AsyncUtils() {}

    /**
     * Runs a task asynchronously with proper error handling and debug logging.
     *
     * @param task the task to run
     * @param description a description of the task for logging purposes
     * @return a CompletableFuture that will complete when the task finishes
     */
    public static <T> CompletableFuture<T> runAsync(Supplier<T> task, String description) {
        if (AsyncConfig.SERVER.enableSuperDebug.get()) {
            LOGGER.info("Starting async task: {}", description);
            logStackTrace("Async task stack trace");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Throwable t) {
                LOGGER.error("Error in async task: " + description, t);
                throw t;
            } finally {
                if (AsyncConfig.SERVER.enableSuperDebug.get()) {
                    LOGGER.info("Completed async task: {}", description);
                }
            }
        }, ASYNC_EXECUTOR);
    }

    /**
     * Runs a task asynchronously with proper error handling and debug logging.
     *
     * @param task the task to run
     * @param description a description of the task for logging purposes
     * @return a CompletableFuture that will complete when the task finishes
     */
    public static CompletableFuture<Void> runAsync(Runnable task, String description) {
        return runAsync(() -> {
            task.run();
            return null;
        }, description);
    }

    /**
     * Logs the current stack trace if super debug mode is enabled.
     *
     * @param message the message to log with the stack trace
     */
    public static void logStackTrace(String message) {
        if (AsyncConfig.SERVER.enableSuperDebug.get()) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder(message).append("\n");
            // Skip the first 2 elements (getStackTrace and this method)
            for (int i = 2; i < Math.min(stackTrace.length, 10); i++) {
                sb.append("    at ").append(stackTrace[i]).append("\n");
            }
            LOGGER.debug(sb.toString());
        }
    }

    /**
     * Schedules a task to run on the server thread.
     * 
     * @param task The task to run on the server thread.
     * @param <T> The return type of the task.
     * @return A CompletableFuture that completes with the result of the task.
     */
    public static <T> CompletableFuture<T> onServerThread(Supplier<T> task) {
        final MinecraftServer server = ForgeEventHandlers.getCurrentServer();
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No server available"));
        }
        
        final CompletableFuture<T> future = new CompletableFuture<>();
        
        server.execute(() -> {
            if (AsyncConfig.SERVER.enableSuperDebug.get()) {
                LOGGER.debug("Executing task on server thread");
            }
            
            try {
                future.complete(task.get());
            } catch (final Throwable t) {
                LOGGER.error("Error in server thread task", t);
                future.completeExceptionally(t);
            }
        });
        
        return future;
    }
    
    /**
     * Schedules a task to run on the server thread.
     * 
     * @param task The task to run on the server thread.
     * @return A CompletableFuture that completes when the task is done.
     */
    public static CompletableFuture<Void> onServerThread(Runnable task) {
        return onServerThread(() -> {
            task.run();
            return null;
        });
    }
    
    /**
     * Gets the server's thread pool executor if available.
     * 
     * @return The server's thread pool executor, or null if not available.
     */
    @Nullable
    public static Executor getServerExecutor() {
        final MinecraftServer server = ForgeEventHandlers.getCurrentServer();
        return server != null ? server : null;
    }
    
    /**
     * Shuts down the async executor. Should be called when the game is shutting down.
     */
    public static void shutdown() {
        if (AsyncConfig.SERVER.enableSuperDebug.get()) {
            LOGGER.info("Shutting down async executor");
        }
        
        ASYNC_EXECUTOR.shutdown();
        
        try {
            // Wait a short time for tasks to complete
            if (!ASYNC_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Async executor did not shut down gracefully, forcing shutdown");
                ASYNC_EXECUTOR.shutdownNow();
            }
        } catch (final InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for async executor to shut down", e);
            Thread.currentThread().interrupt();
        }
    }
}
