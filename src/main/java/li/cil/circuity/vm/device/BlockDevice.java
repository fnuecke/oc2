package li.cil.circuity.vm.device;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface BlockDevice extends Closeable {
    /**
     * Returns whether this device is read-only.
     * <p>
     * When {@code true}, modifying the buffer returned from {@link #getView(long, int)}
     * will throw an {@link UnsupportedOperationException}.
     *
     * @return whether this device is read-only.
     */
    boolean isReadonly();

    /**
     * The overall capacity of this block device in bytes.
     *
     * @return the capacity of the block device.
     */
    long getCapacity();

    /**
     * Get a view on a section of the data in the block device.
     *
     * @param offset the offset of the view.
     * @param length the length of the view.
     * @return the view on the specified section
     * @throws IllegalArgumentException if the interval specified via {@code offset} and
     *                                  {@code length} does not fit into the block device.
     */
    ByteBuffer getView(long offset, int length);

    @Override
    default void close() throws IOException {
    }
}
