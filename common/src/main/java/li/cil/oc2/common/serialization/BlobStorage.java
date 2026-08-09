/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization;

import li.cil.oc2.api.API;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * This class facilitates storing binary chunks of data in an efficient, parallelized fashion.
 */
@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlobStorage {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final LevelResource BLOBS_FOLDER_NAME = new LevelResource(API.MOD_ID + "-blobs");
    private static final Map<UUID, FileChannel> BLOBS = new HashMap<>();

    private static Path dataDirectory; // Directory blobs get saved to.

    ///////////////////////////////////////////////////////////////////

    /**
     * Sets the currently running server.
     * <p>
     * This is used to configure the directory blobs get stored in.
     * <p>
     * We strongly assume that there will never be more than one active Minecraft server per process.
     *
     * @param server the currently active server.
     */
    public static void setServer(final MinecraftServer server) {
        dataDirectory = server.getWorldPath(BLOBS_FOLDER_NAME);
        try {
            Files.createDirectories(dataDirectory);
        } catch (final IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Closes all currently open blobs.
     */
    public static synchronized void close() {
        for (final FileChannel blob : BLOBS.values()) {
            try {
                blob.close();
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }

        BLOBS.clear();
    }

    /**
     * Allocates a new handle for a blob to store.
     * <p>
     * Use this in a call to {@link #getOrOpen(UUID)} to open the blob storage.
     *
     * @return a new handle.
     */
    public static UUID allocateHandle() {
        return UUID.randomUUID();
    }

    /**
     * Validates a blob handle, returning a new one if it is {@code null} or invalid.
     *
     * @param handle the handle to validate.
     * @return the {@code handle} if valid; a new blob handle otherwise.
     */
    public static UUID validateHandle(@Nullable final UUID handle) {
        if (handle == null || (handle.getMostSignificantBits() == 0 && handle.getLeastSignificantBits() == 0)) {
            return allocateHandle();
        } else {
            return handle;
        }
    }

    /**
     * Get or opens a file channel for the blob with the specified handle.
     * <p>
     * The returned file channel supports random access.
     *
     * @param handle the handle to obtain the file channel for.
     * @return the file channel for the requested blob.
     * @throws IOException if opening the blob fails.
     */
    public static synchronized FileChannel getOrOpen(final UUID handle) throws IOException {
        FileChannel blob = BLOBS.get(handle);
        if (blob != null && blob.isOpen()) {
            return blob;
        }

        final Path path = dataDirectory.resolve(handle.toString());
        blob = new RandomAccessFile(path.toFile(), "rw").getChannel();
        BLOBS.put(handle, blob);
        return blob;
    }

    /**
     * Closes the blob with the specified handle.
     *
     * @param handle the handle of the blob to close.
     */
    public static synchronized void close(final UUID handle) {
        try {
            final FileChannel blob = BLOBS.remove(handle);
            if (blob != null) {
                blob.close();
            }
        } catch (final IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Deletes the blob with the specified handle.
     *
     * @param handle the handle of the blob to delete.
     */
    public static void delete(final UUID handle) {
        close(handle);

        final Path path = dataDirectory.resolve(handle.toString());
        CompletableFuture.runAsync(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (final Throwable e) {
                LOGGER.error(e);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void handleServerAboutToStart(final ServerAboutToStartEvent event) {
        BlobStorage.setServer(event.getServer());
    }

    @SubscribeEvent
    public static void handleServerStopped(final ServerStoppedEvent event) {
        BlobStorage.close();
    }
}
