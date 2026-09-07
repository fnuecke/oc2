/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization;

import dev.architectury.event.events.common.LifecycleEvent;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * This class facilitates storing binary chunks of data in an efficient, parallelized fashion.
 * <p>
 * Blobs are referenced by handles that are persisted in item stacks and block entities. Since there is no
 * reliable way of telling whether such a reference still exists anywhere in the world, blobs would grow
 * without bound. To keep disk usage bounded, the number of blobs is capped and the least recently used
 * blob is evicted once that cap is reached. Recency is tracked via the last modified time of the blob
 * file, which is explicitly refreshed whenever a blob is opened or closed.
 * <p>
 * Blobs that are currently open, i.e. in use by a loaded virtual machine, are never evicted.
 */
public final class BlobStorage {
    private static final Logger LOGGER = LogManager.getLogger(BlobStorage.class);

    // --------------------------------------------------------------------- //

    private static final String MARKER_SUFFIX = ".dirty";

    private static final LevelResource BLOBS_FOLDER_NAME = new LevelResource(API.MOD_ID + "-blobs");
    private static final LevelResource TRASH_FOLDER_NAME = new LevelResource(API.MOD_ID + "-blobs-trash");
    private static final UUID INVALID_HANDLE = new UUID(0, 0);

    private static final Map<UUID, FileChannel> BLOBS = new HashMap<>();
    private static final Set<UUID> CLOSED_SINCE_SAVE = new HashSet<>();

    @Nullable
    private static MinecraftServer server; // Server owning the store, for thread checks.
    private static Path dataDirectory; // Directory blobs get saved to.
    @Nullable
    private static Path trashDirectory; // Directory evicted blobs get moved to.
    private static int blobCount; // Number of blobs in dataDirectory.

    // --------------------------------------------------------------------- //

    /**
     * Sets the currently running server.
     * <p>
     * This is used to configure the directory blobs get stored in.
     * <p>
     * We strongly assume that there will never be more than one active Minecraft server per process.
     *
     * @param server the currently active server.
     */
    public static synchronized void setServer(final MinecraftServer server) {
        BlobStorage.server = server;
        dataDirectory = server.getWorldPath(BLOBS_FOLDER_NAME);
        trashDirectory = server.getWorldPath(TRASH_FOLDER_NAME);
        try {
            Files.createDirectories(dataDirectory);
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        blobCount = countBlobs(dataDirectory);
        LOGGER.info("Blob storage in [{}] currently holds {} blob(s).", dataDirectory, blobCount);

        final int markerCount = countMarkers(dataDirectory);
        if (markerCount > 0) {
            LOGGER.warn("{} blob(s) were still mapped when the previous session ended. Their content may not "
                + "match the world that references them, so the devices holding them will refuse to run "
                + "until that is acknowledged.", markerCount);
        }

        final int limit = Config.maxBlobCount;
        if (limit > 0 && blobCount >= limit) {
            LOGGER.warn("Blob storage is at or above the configured limit of {} blob(s). Least recently used " +
                "blobs will be evicted as new ones are created. Raise 'admin.storage.maxBlobCount' or " +
                "set it to 0 to disable this, if you want to keep more blob data.", limit);
        }
    }

    /**
     * Closes all currently open blobs.
     */
    public static synchronized void close() {
        for (final Map.Entry<UUID, FileChannel> entry : BLOBS.entrySet()) {
            touch(entry.getKey());
            try {
                entry.getValue().close();
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }

        CLOSED_SINCE_SAVE.addAll(BLOBS.keySet());
        BLOBS.clear();

        clearMarkersOfClosedBlobs();
    }

    /**
     * Notifies blob storage that the world's references to blobs have been written to disk.
     * <p>
     * Blobs that were closed since the last such point are no longer ahead of the world, so their
     * markers are dropped. Blobs still open keep theirs; they will keep diverging.
     */
    public static synchronized void handleSaved() {
        clearMarkersOfClosedBlobs();
    }

    /**
     * Allocates a new handle for a blob to store.
     * <p>
     * Use this in a call to {@link #open(UUID, boolean)} to open the blob storage.
     *
     * @return a new handle.
     */
    public static UUID allocateHandle() {
        return UUID.randomUUID();
    }

    /**
     * Checks whether the specified handle may refer to a blob.
     *
     * @param handle the handle to check.
     * @return {@code true} if the handle is usable; {@code false} otherwise.
     */
    public static boolean isValidHandle(@Nullable final UUID handle) {
        return handle != null && !INVALID_HANDLE.equals(handle);
    }

    /**
     * Checks whether the blob with the specified handle was left mapped by a previous session.
     * <p>
     * Such a blob may hold data the world does not know about, because the process died before the
     * world was saved again. Stays true until the blob is closed cleanly and the world is saved.
     *
     * @param handle the handle to check.
     * @return {@code true} if the blob outlived a previous session; {@code false} otherwise.
     */
    public static synchronized boolean isStaleHandle(@Nullable final UUID handle) {
        // Open and closed-since-save between them cover every blob this session has touched, because the
        // save that drops a handle from the closed set deletes its marker in the same pass.
        return handle != null && !isOpen(handle) && !CLOSED_SINCE_SAVE.contains(handle) && isMarkedHandle(handle);
    }

    /**
     * Checks whether a blob with the specified handle currently exists on disk.
     *
     * @param handle the handle to check.
     * @return {@code true} if the blob exists; {@code false} otherwise.
     */
    public static synchronized boolean exists(final UUID handle) {
        return dataDirectory != null && Files.exists(pathOf(handle));
    }

    /**
     * Checks whether the blob with the specified handle is currently open.
     * <p>
     * Open blobs are in use by some loaded device and must not be deleted.
     *
     * @param handle the handle to check.
     * @return {@code true} if the blob is open; {@code false} otherwise.
     */
    public static synchronized boolean isOpen(final UUID handle) {
        final FileChannel blob = BLOBS.get(handle);
        return blob != null && blob.isOpen();
    }

    /**
     * Opens a file channel for the blob with the specified handle.
     * <p>
     * The returned file channel supports random access.
     * <p>
     * Opening a blob that is already open is treated as an error, because it means two devices reference
     * the same blob. Should really only happen if the item stack carrying the handle was duplicated. Mapping
     * the same file twice would have the two devices silently corrupt each other's data.
     *
     * @param handle          the handle to obtain the file channel for.
     * @param createIfMissing whether to create the blob if it does not exist yet.
     * @return the file channel for the requested blob.
     * @throws BlobInUseException   if the blob is already open.
     * @throws BlobMissingException if the blob does not exist and {@code createIfMissing} is {@code false}.
     * @throws IOException          if opening the blob fails.
     */
    public static synchronized FileChannel open(final UUID handle, final boolean createIfMissing) throws IOException {
        if (dataDirectory == null) {
            throw new IOException("Blob storage has not been initialized.");
        }

        final FileChannel openBlob = BLOBS.get(handle);
        if (openBlob != null && openBlob.isOpen()) {
            throw new BlobInUseException(handle);
        }

        // Stale entry for a channel that was closed manually; drop it.
        BLOBS.remove(handle);

        final Path path = pathOf(handle);
        final boolean isNew = !Files.exists(path);
        if (isNew) {
            if (!createIfMissing) {
                throw new BlobMissingException(handle);
            }

            evictUntilBelowLimit();
        }

        final FileChannel blob = new RandomAccessFile(path.toFile(), "rw").getChannel();
        if (isNew) {
            blobCount++;
        }

        BLOBS.put(handle, blob);
        CLOSED_SINCE_SAVE.remove(handle);
        touch(handle);
        createMarker(handle);

        return blob;
    }

    /**
     * Closes the blob with the specified handle.
     *
     * @param handle the handle of the blob to close.
     */
    public static synchronized void close(final UUID handle) {
        final FileChannel blob = BLOBS.remove(handle);
        if (blob == null) {
            return;
        }

        touch(handle);
        CLOSED_SINCE_SAVE.add(handle);

        try {
            blob.close();
        } catch (final IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Deletes the blob with the specified handle, unless it is currently in use.
     * <p>
     * A blob that is open belongs to some device that is running right now. Since handles can be
     * duplicated along with the item carrying them, that device is not necessarily the one asking for
     * the deletion, so we leave the blob alone and let eviction deal with it once it goes cold.
     *
     * @param handle the handle of the blob to delete.
     */
    public static synchronized void delete(final UUID handle) {
        if (dataDirectory == null || server == null || !server.isSameThread()) {
            return;
        }

        if (isOpen(handle)) {
            LOGGER.debug("Not deleting blob [{}], it is currently in use.", handle);
            return;
        }

        try {
            if (Files.deleteIfExists(pathOf(handle))) {
                blobCount--;
            }
            Files.deleteIfExists(markerPathOf(handle));
            CLOSED_SINCE_SAVE.remove(handle);
        } catch (final IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * The number of blobs currently held in storage.
     *
     * @return the current blob count.
     */
    public static synchronized int getBlobCount() {
        return blobCount;
    }

    /**
     * The directory blobs are stored in, if a server is currently running.
     *
     * @return the blob directory.
     */
    @Nullable
    public static synchronized Path getDataDirectory() {
        return dataDirectory;
    }

    /**
     * The directory evicted blobs are moved to, if a server is currently running.
     *
     * @return the trash directory.
     */
    @Nullable
    public static synchronized Path getTrashDirectory() {
        return trashDirectory;
    }

    // --------------------------------------------------------------------- //

    public static void initialize() {
        LifecycleEvent.SERVER_BEFORE_START.register(BlobStorage::handleServerAboutToStart);
        LifecycleEvent.SERVER_STOPPED.register(server -> handleServerStopped());
    }

    private static void handleServerAboutToStart(final MinecraftServer server) {
        BlobStorage.setServer(server);
    }

    private static synchronized void handleServerStopped() {
        BlobStorage.close();

        dataDirectory = null;
        trashDirectory = null;
        blobCount = 0;
    }

    // --------------------------------------------------------------------- //

    // Technically internal, public for tests.
    public static synchronized boolean isMarkedHandle(final UUID handle) {
        return dataDirectory != null && Files.exists(markerPathOf(handle));
    }

    private static Path pathOf(final UUID handle) {
        assert dataDirectory != null;
        return dataDirectory.resolve(handle.toString());
    }

    private static void touch(final UUID handle) {
        if (dataDirectory == null) {
            return;
        }

        try {
            final Path path = pathOf(handle);
            if (Files.exists(path)) {
                Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
            }
        } catch (final IOException e) {
            LOGGER.debug("Failed refreshing last use time of blob [{}].", handle, e);
        }
    }

    private static Path markerPathOf(final UUID handle) {
        assert dataDirectory != null;
        return dataDirectory.resolve(handle + MARKER_SUFFIX);
    }

    private static void createMarker(final UUID handle) {
        try {
            final Path path = markerPathOf(handle);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (final IOException e) {
            LOGGER.error("Failed marking blob [{}] as in use.", handle, e);
        }
    }

    private static void clearMarkersOfClosedBlobs() {
        for (final UUID handle : CLOSED_SINCE_SAVE) {
            try {
                Files.deleteIfExists(markerPathOf(handle));
            } catch (final IOException e) {
                LOGGER.error("Failed clearing marker of blob [{}].", handle, e);
            }
        }

        CLOSED_SINCE_SAVE.clear();
    }

    private static int countMarkers(final Path directory) {
        try (Stream<Path> paths = Files.list(directory)) {
            return (int) paths.filter(path -> path.getFileName().toString().endsWith(MARKER_SUFFIX)).count();
        } catch (final IOException e) {
            LOGGER.error(e);
            return 0;
        }
    }

    private static int countBlobs(final Path directory) {
        try (Stream<Path> paths = Files.list(directory)) {
            return (int) paths.filter(BlobStorage::isBlob).count();
        } catch (final IOException e) {
            LOGGER.error(e);
            return 0;
        }
    }

    private static boolean isBlob(final Path path) {
        return Files.isRegularFile(path) && tryParseHandle(path) != null;
    }

    @Nullable
    private static UUID tryParseHandle(final Path path) {
        try {
            return UUID.fromString(path.getFileName().toString());
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private static void evictUntilBelowLimit() throws BlobStorageFullException {
        final int limit = Config.maxBlobCount;
        if (limit <= 0 || blobCount < limit) {
            return;
        }

        final int toEvict = blobCount - limit + 1;

        final List<Candidate> candidates = collectEvictionCandidates();
        candidates.sort(Comparator.comparingLong(Candidate::lastUsedMillis));

        final long now = System.currentTimeMillis();
        int evicted = 0;
        boolean anythingTrashed = false;

        for (final Candidate candidate : candidates) {
            if (evicted >= toEvict) {
                break;
            }

            final boolean trashed;
            try {
                trashed = evict(candidate.handle(), candidate.path());
            } catch (final IOException e) {
                LOGGER.error("Failed evicting blob [{}].", candidate.handle(), e);
                continue;
            }

            anythingTrashed |= trashed;
            blobCount--;
            evicted++;

            LOGGER.info("Evicted blob [{}], unused for {} hour(s), to stay within the configured limit of {} blob(s). {}",
                candidate.handle(),
                TimeUnit.MILLISECONDS.toHours(now - candidate.lastUsedMillis()),
                limit,
                trashed ? "It was moved to [" + trashDirectory + "]." : "It was deleted.");
        }

        if (anythingTrashed) {
            trimTrash();
        }

        if (blobCount >= limit) {
            throw new BlobStorageFullException(limit);
        }
    }

    private static List<Candidate> collectEvictionCandidates() {
        assert dataDirectory != null;

        final long graceMillis = TimeUnit.HOURS.toMillis(Math.max(Config.blobEvictionGraceHours, 0));
        final long now = System.currentTimeMillis();

        final List<Candidate> candidates = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(dataDirectory)) {
            for (final Path path : paths) {
                final UUID handle = tryParseHandle(path);
                if (handle == null) {
                    continue;
                }

                if (isOpen(handle)) {
                    continue;
                }

                final BasicFileAttributes attributes;
                try {
                    attributes = Files.readAttributes(path, BasicFileAttributes.class);
                } catch (final IOException e) {
                    continue;
                }

                if (!attributes.isRegularFile()) {
                    continue;
                }

                final long lastUsed = attributes.lastModifiedTime().toMillis();
                if (now - lastUsed < graceMillis) {
                    continue;
                }

                candidates.add(new Candidate(handle, path, lastUsed));
            }
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        return candidates;
    }

    private static boolean evict(final UUID handle, final Path path) throws IOException {
        final Path marker = markerPathOf(handle);
        CLOSED_SINCE_SAVE.remove(handle);

        if (Config.maxTrashedBlobCount <= 0 || trashDirectory == null) {
            Files.deleteIfExists(path);
            Files.deleteIfExists(marker);
            return false;
        }

        Files.createDirectories(trashDirectory);
        Files.move(path, trashDirectory.resolve(handle.toString()), StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(marker)) {
            Files.move(marker, trashDirectory.resolve(handle + MARKER_SUFFIX), StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    private static void trimTrash() {
        final int limit = Config.maxTrashedBlobCount;
        if (trashDirectory == null || limit <= 0) {
            return;
        }

        final List<Candidate> trashed = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(trashDirectory)) {
            for (final Path path : paths) {
                final UUID handle = tryParseHandle(path);
                if (handle == null) {
                    continue;
                }

                long lastUsed = 0;
                try {
                    final BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                    if (!attributes.isRegularFile()) {
                        continue;
                    }
                    lastUsed = attributes.lastModifiedTime().toMillis();
                } catch (final IOException e) {
                    // Treat as ancient.
                }

                trashed.add(new Candidate(handle, path, lastUsed));
            }
        } catch (final IOException e) {
            LOGGER.error(e);
            return;
        }

        if (trashed.size() <= limit) {
            return;
        }

        trashed.sort(Comparator.comparingLong(Candidate::lastUsedMillis));

        final int toDelete = trashed.size() - limit;
        for (int i = 0; i < toDelete; i++) {
            try {
                Files.deleteIfExists(trashed.get(i).path());
                Files.deleteIfExists(trashDirectory.resolve(trashed.get(i).handle() + MARKER_SUFFIX));
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }
    }

    private record Candidate(UUID handle, Path path, long lastUsedMillis) {
    }

    // --------------------------------------------------------------------- //

    /**
     * Thrown when trying to open a blob that no longer exists, e.g. because it was evicted.
     */
    public static final class BlobMissingException extends IOException {
        public BlobMissingException(final UUID handle) {
            super("No blob with handle [" + handle + "] exists.");
        }
    }

    /**
     * Thrown when trying to open a blob that is already in use by another device.
     */
    public static final class BlobInUseException extends IOException {
        public BlobInUseException(final UUID handle) {
            super("Blob with handle [" + handle + "] is already in use.");
        }
    }

    /**
     * Thrown when a new blob would exceed the configured blob count limit and nothing can be evicted.
     */
    public static final class BlobStorageFullException extends IOException {
        public BlobStorageFullException(final int limit) {
            super("Blob storage is at its limit of " + limit + " blob(s) and no blob is old enough to evict.");
        }
    }
}
