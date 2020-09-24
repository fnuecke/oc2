package li.cil.circuity.vm.device.virtio;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Provides a way of iterating over in virtqueues and obtain their available descriptor chains.
 * <p>
 * Virtqueues work by the driver providing descriptor chains where each descriptor in a chain
 * can either be read-only or write-only. This iterator wraps this through the {@link DescriptorChain}
 * interface which virtually concatenates all descriptors in a chain that are of the same type.
 * Depending on whether the chain contains only read-only descriptors, only write-only descriptors,
 * or some read-only descriptors followed by some write-only descriptors the {@link DescriptorChain}
 * will be readable, writable or writable after readable, respectively.
 * <p>
 * A chain where read-only descriptors follow after one or more write-only descriptors is
 * illegal. When this case is encountered the device will enter an error state and will need
 * a reset.
 */
public interface VirtqueueIterator {
    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     * @throws MemoryAccessException when an exception is thrown while accessing physical memory.
     */
    boolean hasNext() throws MemoryAccessException;

    /**
     * Returns the next descriptor chain in the queue.
     * <p>
     * <b>The caller <em>must</em> call {@link DescriptorChain#use()} on the obtained descriptor chain.</b>
     * <p>
     * The caller may defer calling {@link DescriptorChain#use()} to a later point, but it
     * must call it at some point. If descriptor chains are not marked used, the descriptors in
     * the chain will remain blocked and the driver will not be able to re-use them.
     *
     * @return the next descriptor chain in the queue.
     * @throws VirtIODeviceException  when the device enters an error state.
     * @throws MemoryAccessException  when an exception is thrown while accessing physical memory.
     * @throws NoSuchElementException if the queue has no more available descriptor chains.
     */
    DescriptorChain next() throws VirtIODeviceException, MemoryAccessException;

    /**
     * Performs the given action for each remaining element until all elements
     * have been processed or the action throws an exception.  Actions are
     * performed in the order of iteration, if that order is specified.
     * Exceptions thrown by the action are relayed to the caller.
     *
     * @param action The action to be performed for each element
     * @throws VirtIODeviceException when the device enters an error state.
     * @throws MemoryAccessException when an exception is thrown while accessing physical memory.
     * @throws NullPointerException  if the specified action is null
     * @implSpec The default implementation behaves as if:
     * <pre>{@code
     *     while (hasNext())
     *         action.accept(next());
     * }</pre>
     */
    default void forEachRemaining(final Consumer<DescriptorChain> action) throws VirtIODeviceException, MemoryAccessException {
        Objects.requireNonNull(action);
        while (hasNext())
            action.accept(next());
    }
}
