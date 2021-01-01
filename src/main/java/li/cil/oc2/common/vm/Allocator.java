package li.cil.oc2.common.vm;

import li.cil.oc2.common.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cooperative memory allocation limit enforcement.
 * <p>
 * Call sites must be cooperative and only free claimed memory when actually being sure the
 * allocated memory associated with the claim will be garbage collected.
 */
public final class Allocator {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final HashMap<UUID, Allocation> ALLOCATIONS = new HashMap<>();
    private static long allocated;

    ///////////////////////////////////////////////////////////////////

    /**
     * Creates a new handle that can be used to claim memory.
     *
     * @return a new handle.
     */
    public static UUID createHandle() {
        return UUID.randomUUID();
    }

    /**
     * Tries to claim the specified amount of memory using the specified {@code handle}.
     * <p>
     * Claimed memory <em>must</em> be returned using {@link #freeMemory(UUID)} to prevent leaks.
     *
     * @param handle the handle to use for claiming memory.
     * @param size   the amount of memory to claim.
     * @return {@code true} if the memory was successfully claimed; {@code false} otherwise.
     */
    public static boolean claimMemory(final UUID handle, final int size) {
        if (!checkArgs(handle, size)) {
            return false;
        }
        if (size != 0) {
            ALLOCATIONS.put(handle, new Allocation(size));
            allocated += size;
        }
        return true;
    }

    /**
     * Frees memory that was claimed using the specified handle.
     * <p>
     * Using this if there was no memory claimed with ths handle or if the handle has already been
     * freed does nothing.
     *
     * @param handle the handle to release the claimed memory for.
     */
    public static void freeMemory(final UUID handle) {
        final Allocation allocation = ALLOCATIONS.remove(handle);
        if (allocation != null) {
            allocated -= allocation.size;
        }
    }

    /**
     * Clears all remaining allocations and logs their stack traces.
     */
    public static void resetAndCheckLeaks() {
        if (allocated > 0) {
            for (final Allocation allocation : ALLOCATIONS.values()) {
                // Skip first three: Allocator::claimMemory, Allocation::new, Throwable::getStacktrace
                LOGGER.error(Arrays.stream(allocation.stacktrace).skip(3).map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n  ", "Leaked memory allocation:\n  ", "")));
            }
        }

        ALLOCATIONS.clear();
        allocated = 0;
    }

    ///////////////////////////////////////////////////////////////////

    private static boolean checkArgs(final UUID handle, final int size) {
        if (ALLOCATIONS.containsKey(handle)) {
            throw new IllegalStateException("Handle is already in use. It must be freed before it can be reused.");
        }
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return Config.maxAllocatedData - size >= allocated;
    }

    private static final class Allocation {
        public final int size;
        private final StackTraceElement[] stacktrace;

        private Allocation(final int size) {
            this.size = size;
            this.stacktrace = new Throwable().getStackTrace();
        }
    }
}
