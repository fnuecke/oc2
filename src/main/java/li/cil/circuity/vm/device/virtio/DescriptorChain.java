package li.cil.circuity.vm.device.virtio;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

import java.nio.ByteBuffer;

/**
 * Represents a single descriptor chain in a virtqueue.
 * <p>
 * Instances of this type can be obtained from a {@link VirtqueueIterator} to read from
 * and write to the current descriptor chain of a virtqueue.
 * <p>
 * Descriptor chains where both {@link #readableBytes()} and {@link #writableBytes()} is
 * non-zero must have all their readable bytes processed first, before they can be written
 * to. In other words, {@link #put(byte)} and its batch versions will throw an
 * {@link IllegalStateException} while {@link #readableBytes()} is non-zero.
 * <p>
 * Descriptor chains become invalid once {@link #use()} has been called. Reading
 * from or writing to them afterwards will raise an {@link IllegalStateException}.
 * <p>
 * To write typed data to the descriptor chain it is recommended to use a {@link ByteBuffer}
 * which this data was written to. The data has to be written in little-endian mode.
 * For example:
 * <pre>{@code
 * ByteBuffer buffer = getTemporaryBufferSomehow();
 * buffer.order(ByteOrder.LITTLE_ENDIAN);
 * buffer.putShort(id);
 * buffer.putShort(arg);
 * buffer.putInt(magic);
 * buffer.flip(); // Make data just written readable in buffer.
 * DescriptorChain chain = getChainSomehow();
 * chain.put(buffer);
 * chain.use();
 * }</pre>
 */
public interface DescriptorChain {
    /**
     * Marks the descriptor chain as used.
     * <p>
     * This releases ownership of the descriptor chain and all descriptors inside it back to
     * the driver that provided the chain. As such, <b>this method must be called</b>. Otherwise
     * we will leak descriptors until we eventually run out.
     */
    void use() throws MemoryAccessException;

    /**
     * The number of bytes that can still be read from this descriptor chain.
     * <p>
     * This will decrease as bytes are read from the chain.
     *
     * @return the number of remaining readable bytes in the descriptor chain.
     */
    int readableBytes();

    /**
     * The number of bytes that can still be written to this descriptor chain.
     * <p>
     * This will decrease as bytes are written to the chain.
     * <p>
     * Devices may opt to not completely fill up write-only descriptors before marking the descriptor
     * chain used by calling {@link DescriptorChain#use()}.
     *
     * @return the maximum
     */
    int writableBytes();

    /**
     * Reads a single byte from the descriptor chain.
     *
     * @return the byte read from the descriptor chain.
     * @throws VirtIODeviceException     when the device enters an error state.
     * @throws MemoryAccessException     when an exception is thrown while accessing physical memory.
     * @throws IndexOutOfBoundsException if {@link #readableBytes()} is zero.
     * @throws IllegalStateException     when called after {@link #use()} has been called.
     */
    byte get() throws VirtIODeviceException, MemoryAccessException;

    /**
     * Reads {@code length} bytes from the descriptor chain into {@code dst} at the specified {@code offset}.
     *
     * @param dst    the byte array to copy bytes from the descriptor chain into.
     * @param offset the offset at which to start writing into the byte array.
     * @param length the number of bytes to copy from the descriptor chain.
     * @throws VirtIODeviceException     when the device enters an error state.
     * @throws MemoryAccessException     when an exception is thrown while accessing physical memory.
     * @throws IndexOutOfBoundsException if {@link #readableBytes()} is smaller than {@code length}.
     * @throws IllegalStateException     when called after {@link #use()} has been called.
     */
    void get(byte[] dst, int offset, int length) throws VirtIODeviceException, MemoryAccessException;

    /**
     * Fills the specified buffer with bytes read from the descriptor chain.
     *
     * @param dst the buffer to copy bytes into.
     * @throws VirtIODeviceException     when the device enters an error state.
     * @throws MemoryAccessException     when an exception is thrown while accessing physical memory.
     * @throws IndexOutOfBoundsException if {@link #readableBytes()} is smaller than {@code src.remaining()}.
     * @throws IllegalStateException     when called after {@link #use()} has been called.
     */
    void get(ByteBuffer dst) throws VirtIODeviceException, MemoryAccessException;

    /**
     * Writes a single byte to the descriptor chain.
     *
     * @param value the value to write to the descriptor chain.
     * @throws VirtIODeviceException     when the device enters an error state.
     * @throws MemoryAccessException     when an exception is thrown while accessing physical memory.
     * @throws IndexOutOfBoundsException if {@link #writableBytes()} is zero.
     * @throws IllegalStateException     when called after {@link #use()} has been called or while
     *                                   {@link #readableBytes()} is non-zero.
     */
    void put(byte value) throws VirtIODeviceException, MemoryAccessException;

    /**
     * Writes {@code length} bytes from {@code src} starting at the specified {@code offset} into the descriptor chain.
     *
     * @param src    the byte array from which to copy bytes into descriptor chain.
     * @param offset the offset at which to start reading from the byte array.
     * @param length the number of bytes to copy into the descriptor chain.
     * @throws VirtIODeviceException     when the device enters an error state.
     * @throws MemoryAccessException     when an exception is thrown while accessing physical memory.
     * @throws IndexOutOfBoundsException if {@link #writableBytes()} is smaller than {@code length}.
     * @throws IllegalStateException     when called after {@link #use()} has been called or while
     *                                   {@link #readableBytes()} is non-zero.
     */
    void put(byte[] src, int offset, int length) throws VirtIODeviceException, MemoryAccessException;

    /**
     * Writes all bytes from the specified buffer into the descriptor chain.
     *
     * @param src the buffer to copy bytes from.
     * @throws VirtIODeviceException     when the device enters an error state.
     * @throws MemoryAccessException     when an exception is thrown while accessing physical memory.
     * @throws IndexOutOfBoundsException if {@link #writableBytes()} is smaller than {@code src.remaining()}.
     * @throws IllegalStateException     when called after {@link #use()} has been called or while
     *                                   {@link #readableBytes()} is non-zero.
     */
    void put(ByteBuffer src) throws VirtIODeviceException, MemoryAccessException;
}
