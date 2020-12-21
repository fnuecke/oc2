package li.cil.oc2.common.serialization;

import com.google.common.collect.HashMultimap;
import li.cil.oc2.Constants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This class facilitates storing binary chunks of data in an efficient, parallelized fashion.
 */
public final class BlobStorage {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final String BLOBS_FOLDER_NAME = "oc2-blobs";

    private static final HashMultimap<UUID, Future<Void>> WRITE_HANDLES = HashMultimap.create();
    private static final HashMultimap<UUID, Future<Void>> READ_HANDLES = HashMultimap.create();
    private static final ExecutorService WORKERS = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r, "OC2 BlobStorage Thread");
        thread.setDaemon(false);
        return thread;
    });

    private static Path dataDirectory; // Directory blobs get saved to.

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(BlobStorage::synchronize));
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Represents a handle to a serialization or deserialization job submitted using
     * {@link #submitSave(UUID, InputStream)} or {@link #submitLoad(UUID, OutputStream)}.
     * <p>
     * This can be used to guarantee the serialization job has been completed before making changes to
     * the to-be-serialized data/the deserialization job has been completed before accessing the to-be-
     * deserialized data.
     */
    public static final class JobHandle {
        private final Set<Future<Void>> futures;

        private JobHandle(final Future<Void> future) {
            this.futures = new HashSet<>();
            futures.add(future);
        }

        private JobHandle(final Set<Future<Void>> futures) {
            this.futures = futures;
        }

        /**
         * Blocks until the jobs described by this job handle are complete.
         */
        public void await() {
            for (final Future<Void> future : futures) {
                try {
                    future.get();
                } catch (final Throwable e) {
                    LOGGER.error("Scheduled blob storage job threw an exception.", e);
                }
            }
            futures.clear();
        }

        /**
         * Combines a list of job handles into a single job handle.
         * <p>
         * This can make awaiting multiple job handles more convenient by simply rolling them
         * into one job handle.
         * <p>
         * For convenience the list of job handles may contain {@code null} entries.
         *
         * @param jobHandles the job handles to merge into a single job handle.
         * @return a job handle that can be used to await all passed job handles.
         */
        public static JobHandle combine(final JobHandle... jobHandles) {
            final HashSet<Future<Void>> futures = new HashSet<>();
            for (final JobHandle jobHandle : jobHandles) {
                if (jobHandle == null) {
                    continue;
                }
                futures.addAll(jobHandle.futures);
            }
            return new JobHandle(futures);
        }
    }

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
        synchronize();
        dataDirectory = server.getSavePath(WorldSavePath.ROOT).resolve(BLOBS_FOLDER_NAME);
        try {
            Files.createDirectories(dataDirectory);
        } catch (final IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Blocks until all currently in-flight serialization or deserialization jobs are completed and
     * deletes all blobs marked for deletion.
     */
    public static void synchronize() {
        awaitCompletion(READ_HANDLES);
        awaitCompletion(WRITE_HANDLES);
    }

    /**
     * Allocates a new handle for a blob to store.
     * <p>
     * Use this in a call to {@link #submitSave(UUID, InputStream)} to write data into blob storage.
     * Then in a call to {@link #submitLoad(UUID, OutputStream)} to read from blob storage.
     *
     * @return a new handle.
     */
    public static UUID allocateHandle() {
        return UUID.randomUUID();
    }

    /**
     * Frees a handle, marking the data associated with it for deletion.
     * <p>
     * Deletions are enacted upon the next call to {@link #synchronize()}, which is called when
     * the server is shut down.
     * <p>
     * Validating this handle or submitting a serialization or deserialization for this handle
     * will "revive" the handle and the data associated with it will <em>not</em> be deleted with
     * the next call to {@link #synchronize()}.
     *
     * @param handle the handle to free.
     */
    public static void freeHandle(@Nullable final UUID handle) {
        if (handle != null) {
            awaitCompletion(READ_HANDLES, handle);
            awaitCompletion(WRITE_HANDLES, handle);

            submitJob(WRITE_HANDLES, handle, () -> delete(handle));
        }
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
     * Submits data to be saved to blob storage, identified by the specified handle.
     * <p>
     * The given input stream should support batch reading operations for best performance.
     * A buffer will be used for copying, so it is not necessary to pass a buffered stream.
     * <p>
     * The returned {@link JobHandle} should be waited on before any changes are made to
     * the to-be-written data after this call returns.
     * <p>
     * <b>The given input stream will be accessed from a worker thread.</b>
     *
     * @param handle     the handle identifying the stored blob data.
     * @param dataAccess a stream used to read the data to be written to blob storage.
     * @return a job handle that can be used to wait for the data to be completely written.
     */
    public static JobHandle submitSave(final UUID handle, final InputStream dataAccess) {
        // No other job must access a handle when it is being written to.
        awaitCompletion(READ_HANDLES, handle);
        awaitCompletion(WRITE_HANDLES, handle);

        return submitJob(WRITE_HANDLES, handle, () -> save(handle, dataAccess));
    }

    /**
     * Submits data to be saved to blob storage, identified by the specified handle.
     * <p>
     * The given input stream should support batch reading operations for best performance.
     * A buffer will be used for copying, so it is not necessary to pass a buffered stream.
     * <p>
     * The returned {@link JobHandle} should be waited on before any changes are made to
     * the to-be-written data after this call returns.
     * <p>
     * <b>The given input stream will be accessed from a worker thread.</b>
     *
     * @param handle     the handle identifying the stored blob data.
     * @param dataAccess a stream used to read the data to be written to blob storage.
     * @return a job handle that can be used to wait for the data to be completely written.
     */
    public static JobHandle submitLoad(final UUID handle, final OutputStream dataAccess) {
        // Multiple jobs may read from a handle at a time but none may write to it when reading from it.
        awaitCompletion(WRITE_HANDLES, handle);

        return submitJob(READ_HANDLES, handle, () -> load(handle, dataAccess));
    }

    ///////////////////////////////////////////////////////////////////

    private static void save(final UUID handle, final InputStream input) {
        try {
            final Path path = dataDirectory.resolve(handle.toString());
            try (final OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                final GZIPOutputStream zipped = new GZIPOutputStream(output);
                copyData(input, zipped);
                zipped.finish();
            }
        } catch (final Throwable e) {
            LOGGER.error(e);
        } finally {
            try {
                input.close();
            } catch (final Throwable e) {
                LOGGER.error(e);
            }
        }
    }

    private static void load(final UUID handle, final OutputStream output) {
        try {
            final Path path = dataDirectory.resolve(handle.toString());
            if (!Files.exists(path)) {
                return;
            }
            try (final InputStream input = Files.newInputStream(path, StandardOpenOption.READ)) {
                final GZIPInputStream zipped = new GZIPInputStream(input);
                copyData(zipped, output);
            }
        } catch (final Throwable e) {
            LOGGER.error(e);
        } finally {
            try {
                output.close();
            } catch (final Throwable e) {
                LOGGER.error(e);
            }
        }
    }

    private static void delete(final UUID handle) {
        try {
            final Path path = dataDirectory.resolve(handle.toString());
            Files.deleteIfExists(path);
        } catch (final Throwable e) {
            LOGGER.error(e);
        }
    }

    private static void copyData(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[8 * Constants.KILOBYTE];
        int count;
        while ((count = in.read(buffer)) > 0) {
            out.write(buffer, 0, count);
        }
    }

    private static JobHandle submitJob(final HashMultimap<UUID, Future<Void>> map, final UUID handle, final Runnable runnable) {
        final CompletableFuture<Void> future = CompletableFuture.runAsync(runnable, WORKERS);
        map.put(handle, future);
        return new JobHandle(future.thenAccept(unused -> completeJob(map, handle, future)));
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter") // I know what I'm doing - famous last words.
    private static void completeJob(final HashMultimap<UUID, Future<Void>> handles, final UUID handle, final Future<Void> future) {
        synchronized (handles) {
            handles.remove(handle, future);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter") // I know what I'm doing - famous last words.
    private static void awaitCompletion(final HashMultimap<UUID, Future<Void>> handles, final UUID handle) {
        // Jobs remove themselves from the list on completion, so we must synchronize access to
        // the map of pending jobs, but must release the lock when waiting for the job so as not
        // to create a deadlock.
        final Set<Future<Void>> futures;
        synchronized (handles) {
            futures = handles.removeAll(handle);
        }

        for (final Future<Void> future : futures) {
            await(future);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter") // I know what I'm doing - famous last words.
    private static void awaitCompletion(final HashMultimap<UUID, Future<Void>> handles) {
        // Jobs remove themselves from the list on completion, so we must synchronize access to
        // the map of pending jobs, but must release the lock when waiting for the job so as not
        // to create a deadlock.
        final Set<Future<Void>> futures;
        synchronized (handles) {
            futures = new HashSet<>(handles.values());
            handles.clear();
        }

        for (final Future<Void> future : futures) {
            await(future);
        }
    }

    private static void await(final Future<Void> future) {
        try {
            future.get();
        } catch (final Throwable e) {
            LOGGER.error("Scheduled blob storage job threw an exception.", e);
        }
    }
}
