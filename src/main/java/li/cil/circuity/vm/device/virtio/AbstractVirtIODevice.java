package li.cil.circuity.vm.device.virtio;

import li.cil.circuity.api.vm.Interrupt;
import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.InterruptSource;
import li.cil.circuity.api.vm.device.Resettable;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.device.memory.Sizes;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

/**
 * Base class for VirtIO devices taking care of common functionality.
 * <p>
 * For setting up the configuration space of a device, consider using these utility functions:
 * <ul>
 *     <li>{@link #setConfigValue(int, byte)}</li>
 *     <li>{@link #setConfigValue(int, short)}</li>
 *     <li>{@link #setConfigValue(int, int)}</li>
 * </ul>
 * <p>
 * For config fields which act as triggers, {@link #loadConfig(int, int)} and {@link #storeConfig(int, int, int)}
 * may be overridden to intercept such writes and respond to them accordingly.
 * <p>
 * This class abstracts the underlying virtqueue system and provides an iterator that virtually
 * concatenates all descriptors provided by the driver, the {@link VirtqueueIterator}. It can
 * be used to easily read from and write to the virtqueue as well as advance it. Use {@link #getQueueIterator(int)}
 * to obtain the iterator for a virtqueue.
 * <p>
 * Queue notifications are forwarded to {@link #handleQueueNotification(int)}, except for virtqueues
 * where this has been explicitly disabled by calling {@link #setQueueNotifications(int, boolean)}.
 */
public abstract class AbstractVirtIODevice implements MemoryMappedDevice, InterruptSource, Resettable {
    protected static final int VIRTIO_VENDOR_ID_GENERIC = 0xFFFF;

    protected static final long VIRTIO_F_RING_INDIRECT_DESC = (1L << 28); // Support for VIRTQ_DESC_F_INDIRECT.
    protected static final long VIRTIO_F_RING_EVENT_IDX = (1L << 29); // Enables 'used_event", 'avail_event'.
    protected static final long VIRTIO_F_VERSION_1 = (1L << 32); // Compliance with v1.1 spec.
    protected static final long VIRTIO_F_ACCESS_PLATFORM = (1L << 33); // Behind IOMMU.
    protected static final long VIRTIO_F_RING_PACKED = (1L << 34); // Support for packed layout.
    protected static final long VIRTIO_F_IN_ORDER = (1L << 35); // Queues are used in order.
    protected static final long VIRTIO_F_ORDER_PLATFORM = (1L << 36); // Need barriers.
    protected static final long VIRTIO_F_SR_IOV = (1L << 37); // Single root I/O virtualization.
    protected static final long VIRTIO_F_NOTIFICATION_DATA = (1L << 38); // Extra info in queue notifications.

    protected static final int VIRTIO_STATUS_ACKNOWLEDGE = 1;
    protected static final int VIRTIO_STATUS_DRIVER = 2;
    protected static final int VIRTIO_STATUS_DRIVER_OK = 4;
    protected static final int VIRTIO_STATUS_FEATURES_OK = 8;
    protected static final int VIRTIO_STATUS_DEVICE_NEEDS_RESET = 64;
    protected static final int VIRTIO_STATUS_FAILED = 128;

    private static final int VIRTIO_IRQ_USED_BUFFER_MASK = 0b01;
    private static final int VIRTIO_IRQ_CONFIGURATION_CHANGE_MASK = 0b10;

    private static final int VIRTIO_MAGIC = 0x74726976; // "virt"
    private static final int VIRTIO_VERSION = 0x2;

    private static final int VIRTIO_MMIO_MAGIC = 0x000; // R
    private static final int VIRTIO_MMIO_VERSION = 0x004; // R
    private static final int VIRTIO_MMIO_DEVICE_ID = 0x008; // R
    private static final int VIRTIO_MMIO_VENDOR_ID = 0x00C; // R
    private static final int VIRTIO_MMIO_DEVICE_FEATURES = 0x010; // R
    private static final int VIRTIO_MMIO_DEVICE_FEATURES_SEL = 0x014; // W
    private static final int VIRTIO_MMIO_DRIVER_FEATURES = 0x020; // W
    private static final int VIRTIO_MMIO_DRIVER_FEATURES_SEL = 0x024; // W
    private static final int VIRTIO_MMIO_QUEUE_SEL = 0x030; // W
    private static final int VIRTIO_MMIO_QUEUE_NUM_MAX = 0x034; // R
    private static final int VIRTIO_MMIO_QUEUE_NUM = 0x038; // W
    private static final int VIRTIO_MMIO_QUEUE_READY = 0x044; // RW
    private static final int VIRTIO_MMIO_QUEUE_NOTIFY = 0x050; // W
    private static final int VIRTIO_MMIO_INTERRUPT_STATUS = 0x060; // R
    private static final int VIRTIO_MMIO_INTERRUPT_ACK = 0x064; // W
    private static final int VIRTIO_MMIO_STATUS = 0x070; // RW
    private static final int VIRTIO_MMIO_QUEUE_DESC_LOW = 0x080; // W
    private static final int VIRTIO_MMIO_QUEUE_DESC_HIGH = 0x084; // W
    private static final int VIRTIO_MMIO_QUEUE_DRIVER_LOW = 0x090; // W
    private static final int VIRTIO_MMIO_QUEUE_DRIVER_HIGH = 0x094; // W
    private static final int VIRTIO_MMIO_QUEUE_DEVICE_LOW = 0x0A0; // W
    private static final int VIRTIO_MMIO_QUEUE_DEVICE_HIGH = 0x0A4; // W
    private static final int VIRTIO_MMIO_CONFIG_GENERATION = 0x0FC; // R
    private static final int VIRTIO_MMIO_CONFIG = 0x100; // RW

    private static final int VIRTQ_MAX_QUEUE_SIZE = 16; // Size of descriptor rings.
    private static final int VIRTQ_MAX_CHAIN_LENGTH = 128; // Max chain length because we don't trust drivers.

    private final MemoryMap memoryMap;
    private final VirtIODeviceSpec spec;
    private final ByteBuffer configuration;
    private final Interrupt interrupt = new Interrupt();
    private final AbstractVirtqueue[] queues;

    private int status = 0;
    private int interruptStatus = 0;
    private int deviceFeaturesSel;
    private long driverFeatures;
    private int driverFeaturesSel;
    private int queueSel;
    private int configGeneration;

    protected AbstractVirtIODevice(final MemoryMap memoryMap, final VirtIODeviceSpec spec) {
        this.memoryMap = memoryMap;
        this.spec = spec;
        this.configuration = ByteBuffer.allocate(spec.configSpaceSizeInBytes);
        configuration.order(ByteOrder.LITTLE_ENDIAN);
        queues = new AbstractVirtqueue[spec.virtQueueCount];

        reset();
    }

    public Interrupt getInterrupt() {
        return interrupt;
    }

    ///////////////////////////////////////////////////////////////////
    // Configuration

    /**
     * This is called upon construction of an instance to request initialization of the config space.
     * <p>
     * The provided buffer will have the length specified in the {@link VirtIODeviceSpec} used when
     * constructing this instance.
     * <p>
     * Future changes to the config space must be signalled by calling {@link #notifyConfigChanged()}.
     * <p>
     * Use {@link #getConfiguration()} to access the raw config space, or use the utility methods to
     * initialize configuration values.
     *
     * @see #getConfiguration()
     */
    protected void initializeConfig() {
    }

    /**
     * Get the buffer storing device configuration.
     *
     * @return the buffer with device configuration.
     */
    protected final ByteBuffer getConfiguration() {
        return configuration;
    }

    /**
     * Sets an 8 bit value in the config space at the specified offset to the specified value.
     * <p>
     * This will automatically call {@link #notifyConfigChanged()}.
     *
     * @param offset the offset into the config space to write at.
     * @param value  the value to write.
     */
    protected final void setConfigValue(final int offset, final byte value) {
        configuration.put(offset, value);
        notifyConfigChanged();
    }

    /**
     * Sets a 16 bit value in the config space at the specified offset to the specified value.
     * <p>
     * This will automatically call {@link #notifyConfigChanged()}.
     *
     * @param offset the offset into the config space to write at.
     * @param value  the value to write.
     */
    protected final void setConfigValue(final int offset, final short value) {
        configuration.putShort(offset, value);
        notifyConfigChanged();
    }

    /**
     * Sets a 32 bit value in the config space at the specified offset to the specified value.
     * <p>
     * This will automatically call {@link #notifyConfigChanged()}.
     *
     * @param offset the offset into the config space to write at.
     * @param value  the value to write.
     */
    protected final void setConfigValue(final int offset, final int value) {
        configuration.putInt(offset, value);
        notifyConfigChanged();
    }

    /**
     * Reads a value from the config space of this device.
     * <p>
     * Override this for computed/dynamic config space values.
     *
     * @param offset   the offset into the config space the load happens at.
     * @param sizeLog2 the width of the value to read in bytes, log2. See {@link Sizes}.
     * @return the value in the config space at the specified offset.
     * @see Sizes
     */
    protected int loadConfig(final int offset, final int sizeLog2) {
        switch (sizeLog2) {
            case Sizes.SIZE_8_LOG2: {
                if (offset >= 0 && offset < configuration.limit()) {
                    return configuration.get(offset);
                }
                break;
            }
            case Sizes.SIZE_16_LOG2: {
                if (offset >= 0 && offset < configuration.limit() - 1) {
                    return configuration.getShort(offset);
                }
                break;
            }
            case Sizes.SIZE_32_LOG2: {
                if (offset >= 0 && offset < configuration.limit() - 3) {
                    return configuration.getInt(offset);
                }
                break;
            }
            case Sizes.SIZE_64_LOG2: {
                if (offset >= 0 && offset < configuration.limit() - 3) {
                    // TODO Widen API to support 64 bit values and addresses.
                    return (int) configuration.getLong(offset);
                }
                break;
            }
        }
        return 0;
    }

    /**
     * Writes a value to the config space of this device.
     * <p>
     * Override this for config space fields that should act as triggers.
     *
     * @param offset   the offset into the config space the load happens at.
     * @param value    the value to write to the config space.
     * @param sizeLog2 the width of the value to write in bytes, log2. See {@link Sizes}.
     * @see Sizes
     */
    protected void storeConfig(final int offset, final int value, final int sizeLog2) {
        switch (sizeLog2) {
            case Sizes.SIZE_8_LOG2: {
                if (offset >= 0 && offset < configuration.limit()) {
                    configuration.put(offset, (byte) value);
                }
                break;
            }
            case Sizes.SIZE_16_LOG2: {
                if (offset >= 0 && offset < configuration.limit() - 1) {
                    configuration.putShort(offset, (short) value);
                }
                break;
            }
            case Sizes.SIZE_32_LOG2: {
                if (offset >= 0 && offset < configuration.limit() - 3) {
                    configuration.putInt(offset, value);
                }
                break;
            }
            case Sizes.SIZE_64_LOG2: {
                if (offset >= 0 && offset < configuration.limit() - 3) {
                    // TODO Widen API to support 64 bit values and addresses.
                    configuration.putLong(offset, value);
                }
                break;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Initialization hooks

    /**
     * Called when the device has been acknowledged by an OS.
     * <p>
     * <b>This is may be called from a worker thread</b>.
     */
    protected void handleDeviceAcknowledged() {
    }

    /**
     * Called when the device has been notified that the OS has a driver for it.
     * <p>
     * <b>This is may be called from a worker thread</b>.
     */
    protected void handleDeviceDriverPresent() {
    }

    /**
     * Called after feature set has been successfully negotiated with the driver.
     * <p>
     * <b>This is may be called from a worker thread</b>.
     */
    protected void handleFeaturesNegotiated() {
    }

    /**
     * Called when device setup has been completed and the driver assumes the device is ready to be used.
     * <p>
     * <b>This is may be called from a worker thread</b>.
     */
    protected void handleDeviceSetupComplete() {
    }

    /**
     * Called when device setup has failed for some reason.
     * <p>
     * <b>This is may be called from a worker thread</b>.
     */
    protected void handleDeviceSetupFailed() {
    }

    /**
     * Allows verification of feature set negotiated by the driver
     * <p>
     * Returning false here allows denying the feature configuration, marking the device as unusable.
     *
     * @param features the features to verify.
     * @return {@code true} if the feature set is valid; {@code false} otherwise.
     */
    protected boolean isFeatureSubsetSupported(final long features) {
        // 6.2: may fail if VIRTIO_F_VERSION_1 is not accepted. So we do.
        if ((features & VIRTIO_F_VERSION_1) == 0) {
            return false;
        }

        return true;
    }

    ///////////////////////////////////////////////////////////////////
    // State and virtqueue access

    /**
     * Get the mask defining the current status of the device.
     * <p>
     * Check for specific status bits using these constants:
     * <ul>
     *     <li>{@link #VIRTIO_STATUS_ACKNOWLEDGE}</li>
     *     <li>{@link #VIRTIO_STATUS_DRIVER}</li>
     *     <li>{@link #VIRTIO_STATUS_DRIVER_OK}</li>
     *     <li>{@link #VIRTIO_STATUS_FEATURES_OK}</li>
     *     <li>{@link #VIRTIO_STATUS_DEVICE_NEEDS_RESET}</li>
     *     <li>{@link #VIRTIO_STATUS_FAILED}</li>
     * </ul>
     * <p>
     * Devices in a failed state should perform any more operations on their virtqueues
     * and internal state.
     *
     * @return the current status mask of the device.
     */
    protected final int getStatus() {
        return status;
    }

    /**
     * Gets the set of features negotiated with a driver.
     * <p>
     * This is only valid once {@link #handleFeaturesNegotiated()} is called and until the next reset.
     *
     * @return the currently negotiated feature set.
     */
    protected final long getNegotiatedFeatures() {
        return spec.features & driverFeatures;
    }

    /**
     * Call this to signal that the config space of the device has changed.
     */
    protected final void notifyConfigChanged() {
        if ((status & VIRTIO_STATUS_ACKNOWLEDGE) == 0) {
            return; // Don't trigger updates when in reset state.
        }

        // 4.2.2.1: Change config generation when config changed.
        configGeneration++;
        interruptStatus |= VIRTIO_IRQ_CONFIGURATION_CHANGE_MASK;
        updateInterrupts();
    }

    /**
     * Call this to signal that the device has entered an error state and needs a reset.
     */
    protected final void error() {
        status |= VIRTIO_STATUS_DEVICE_NEEDS_RESET;

        // 2.1.2: After setting DEVICE_NEEDS_RESET must send config change notification.
        notifyConfigChanged();
    }

    ///////////////////////////////////////////////////////////////////
    // Virtqueues

    /**
     * Returns an iterator that allows iteration over the virtqueue at the specified index.
     * <p>
     * Use this to read from and write to virtqueues and consume descriptor chains in them.
     * <p>
     * The {@code queueIndex} in an index into the list of virtqueues allocated based on the number
     * of virtqueues requested in the device's {@link VirtIODeviceSpec}.
     * <p>
     * This may return {@code null} in case the device has been reset and not
     * yet reach {@code FEATURES_OK} state, signalled by {@link #handleFeaturesNegotiated()}
     * being called.
     *
     * @param queueIndex the index of the virtqueue to get.
     * @return the virtqueue at the specified index.
     */
    @Nullable
    protected final VirtqueueIterator getQueueIterator(final int queueIndex) {
        if ((status & VIRTIO_STATUS_FEATURES_OK) == 0) {
            return null;
        }
        return queues[queueIndex];
    }

    /**
     * Called when descriptors in the queue with the specified index become available.
     * <p>
     * The {@code queueIndex} in an index into the list of virtqueues allocated based on the number
     * of virtqueues requested in the device's {@link VirtIODeviceSpec}.
     * <p>
     * Read data from or write data to the queue using a {@link VirtqueueIterator}. Notification
     * handlers may opt to not process available descriptors immediately and process the queue at
     * a later point.
     * <p>
     * It is inadvisable to rely on this method getting called. It is up to the driver whether this
     * notification is sent or not. In particular, it is inadvisable to rely on this method getting
     * called <em>again</em> for this queue unless a {@link DescriptorChain} obtained from the queue's
     * {@link VirtqueueIterator} was marked as used by calling {@link DescriptorChain#use()}. The
     * driver may not send additional notifications if the queue is full, for example.
     * <p>
     * <b>This is may be called from a worker thread</b>.
     *
     * @param queueIndex the index of the queue for which a notification was received.
     * @throws VirtIODeviceException when the device enters an error state.
     * @throws MemoryAccessException when an exception is thrown while accessing physical memory.
     * @see #getQueueIterator(int)
     */
    protected void handleQueueNotification(final int queueIndex) throws VirtIODeviceException, MemoryAccessException {
    }

    /**
     * May be used to enable or disable queue notifications via {@link #handleQueueNotification(int)}.
     * <p>
     * Use this to disable notifications when they would always be ignored e.g. because this data will
     * be processed at some other time.
     * <p>
     * Since queues may be recreated after a device reset due to changes in the negotiated feature
     * set, it is recommended to call this method from {@link #handleFeaturesNegotiated()}.
     *
     * @param queueIndex the index of the queue to disable notifications for.
     * @param enabled    {@code true} to enable notifications; {@code false} otherwise.
     */
    protected final void setQueueNotifications(final int queueIndex, final boolean enabled) {
        if ((status & VIRTIO_STATUS_FEATURES_OK) == 0) {
            return;
        }
        queues[queueIndex].dispatchQueueNotifications = enabled;
    }

    /**
     * Ensures that either the passed {@code chain} is a valid read-only descriptor chain and
     * returns it, or tries to fetch the next read-only descriptor chain from the queue with
     * the specified index.
     * <p>
     * If the passed descriptor chain has no more readable bytes {@link DescriptorChain#use()}
     * will be called on the chain before trying to obtain the next read-only descriptor chain
     * from the queue with the specified index.
     * <p>
     * Use this when the specified queue is required to only contain read-only descriptor chains.
     * This method will throw a {@link VirtIODeviceException} if a descriptor chain with write-
     * only descriptors is encountered in the queue with the specified index.
     *
     * @param queueIndex the index to try to obtain a read-only descriptor chain from if the
     *                   passed descriptor chain has no readable bytes remaining.
     * @param chain      the descriptor chain to validate.
     * @return a read-only descriptor chain with readable bytes; {@code null} if none is available.
     * @throws VirtIODeviceException when the device enters an error state.
     * @throws MemoryAccessException when an exception is thrown while accessing physical memory.
     */
    @Nullable
    protected final DescriptorChain validateReadOnlyDescriptorChain(final int queueIndex, final DescriptorChain chain) throws VirtIODeviceException, MemoryAccessException {
        if (chain != null) {
            if (chain.readableBytes() > 0) {
                return chain;
            } else {
                chain.use();
            }
        }

        final VirtqueueIterator queue = getQueueIterator(queueIndex);
        if (queue == null) {
            return null;
        }

        while (queue.hasNext()) {
            final DescriptorChain newChain = queue.next();

            if (newChain.writableBytes() > 0) {
                error();
                return null;
            }

            if (newChain.readableBytes() > 0) {
                return newChain;
            }

            newChain.use();
        }

        return null;
    }

    /**
     * Ensures that either the passed {@code chain} is a valid write-only descriptor chain and
     * returns it, or tries to fetch the next write-only descriptor chain from the queue with
     * the specified index.
     * <p>
     * If the passed descriptor chain has no more writable bytes {@link DescriptorChain#use()}
     * will be called on the chain before trying to obtain the next write-only descriptor chain
     * from the queue with the specified index.
     * <p>
     * Use this when the specified queue is required to only contain write-only descriptor chains.
     * This method will throw a {@link VirtIODeviceException} if a descriptor chain with read-
     * only descriptors is encountered in the queue with the specified index.
     *
     * @param queueIndex the index to try to obtain a write-only descriptor chain from if the
     *                   passed descriptor chain has no writable bytes remaining.
     * @param chain      the descriptor chain to validate.
     * @return a write-only descriptor chain with writable bytes; {@code null} if none is available.
     * @throws VirtIODeviceException when the device enters an error state.
     * @throws MemoryAccessException when an exception is thrown while accessing physical memory.
     */
    @Nullable
    protected final DescriptorChain validateWriteOnlyDescriptorChain(final int queueIndex, final DescriptorChain chain) throws VirtIODeviceException, MemoryAccessException {
        if (chain != null) {
            if (chain.writableBytes() > 0) {
                return chain;
            } else {
                chain.use();
            }
        }

        final VirtqueueIterator queue = getQueueIterator(queueIndex);
        if (queue == null) {
            return null;
        }

        while (queue.hasNext()) {
            final DescriptorChain newChain = queue.next();

            if (newChain.readableBytes() > 0) {
                error();
                throw new VirtIODeviceException();
            }

            if (newChain.writableBytes() > 0) {
                return newChain;
            }

            newChain.use();
        }

        return null;
    }

    ///////////////////////////////////////////////////////////////////
    // MemoryMappedDevice

    @Override
    public final int getLength() {
        return 0x100 + configuration.capacity();
    }

    @Override
    public final int load(final int offset, final int sizeLog2) {
        if (offset >= VIRTIO_MMIO_CONFIG) {
            return loadConfig(offset - VIRTIO_MMIO_CONFIG, sizeLog2);
        }

        if (sizeLog2 != Sizes.SIZE_32_LOG2) {
            return 0;
        }

        switch (offset) {
            case VIRTIO_MMIO_MAGIC: {
                // 4.2.2.1: Must return magic value.
                return VIRTIO_MAGIC;
            }
            case VIRTIO_MMIO_VERSION: {
                // 4.2.2.1: Must return version 2.
                return VIRTIO_VERSION;
            }
            case VIRTIO_MMIO_DEVICE_ID: {
                return spec.deviceId;
            }
            case VIRTIO_MMIO_VENDOR_ID: {
                return spec.vendorId;
            }
            case VIRTIO_MMIO_DEVICE_FEATURES: {
                // We only support 64 feature bits.
                if (Long.compareUnsigned(deviceFeaturesSel, 1) <= 0) {
                    final int shift = deviceFeaturesSel * 32;
                    return (int) (spec.features >>> shift);
                }
            }
            case VIRTIO_MMIO_QUEUE_NUM_MAX: {
                return VIRTQ_MAX_QUEUE_SIZE;
            }
            case VIRTIO_MMIO_QUEUE_READY: {
                return queues[queueSel].ready;
            }
            case VIRTIO_MMIO_INTERRUPT_STATUS: {
                return interruptStatus;
            }
            case VIRTIO_MMIO_STATUS: {
                return status;
            }
            case VIRTIO_MMIO_CONFIG_GENERATION: {
                return configGeneration;
            }
        }

        return 0;
    }

    @Override
    public final void store(final int offset, final int value, final int sizeLog2) {
        if (offset >= VIRTIO_MMIO_CONFIG) {
            storeConfig(offset - VIRTIO_MMIO_CONFIG, value, sizeLog2);
            return;
        }

        if (sizeLog2 != Sizes.SIZE_32_LOG2) {
            return;
        }

        switch (offset) {
            case VIRTIO_MMIO_DEVICE_FEATURES_SEL: {
                deviceFeaturesSel = value;
                break;
            }

            case VIRTIO_MMIO_DRIVER_FEATURES: {
                // We only support 64 feature bits.
                if (Long.compareUnsigned(driverFeaturesSel, 1) <= 0) {
                    final int shift = driverFeaturesSel * 32;
                    final long mask = 0xFFFFFFFFL;
                    driverFeatures = (driverFeatures & ~(mask << shift)) | (((long) value & mask) << shift);
                }
                break;
            }
            case VIRTIO_MMIO_DRIVER_FEATURES_SEL: {
                driverFeaturesSel = value;
                break;
            }

            case VIRTIO_MMIO_QUEUE_SEL: {
                if (Integer.compareUnsigned(value, queues.length) < 0) {
                    queueSel = value;
                }
                break;
            }
            case VIRTIO_MMIO_QUEUE_NUM: {
                // 2.6: Queue size is always a power of 2. The maximum Queue Size value is 32768.
                if (value <= (1 << 15) && Integer.bitCount(value) == 1) {
                    queues[queueSel].num = value;
                }
                break;
            }
            case VIRTIO_MMIO_QUEUE_READY: {
                queues[queueSel].ready = value != 0 ? 1 : 0;
                break;
            }
            case VIRTIO_MMIO_QUEUE_NOTIFY: {
                // 3.1.1: Driver must not send buffer available notifications before DRIVER_OK.
                if ((status & VIRTIO_STATUS_DRIVER_OK) == 0) {
                    error();
                    return;
                }

                if (Integer.compareUnsigned(value, queues.length) < 0) {
                    try {
                        queues[value].handleQueueNotification(value);
                    } catch (final VirtIODeviceException | MemoryAccessException e) {
                        error();
                    }
                }
                break;
            }

            case VIRTIO_MMIO_INTERRUPT_ACK: {
                interruptStatus &= ~value;
                updateInterrupts();
                break;
            }

            case VIRTIO_MMIO_STATUS: {
                final int change = status ^ value;
                status = value;

                // Initialization sequence:
                // 1. Reset.
                // 2. ACKNOWLEDGE bit set.
                if ((change & status & VIRTIO_STATUS_ACKNOWLEDGE) != 0) {
                    handleDeviceAcknowledged();
                }

                // 3. DRIVER bit set.
                if ((change & status & VIRTIO_STATUS_DRIVER) != 0) {
                    handleDeviceDriverPresent();
                }

                // 4. Feature bits read and written.
                // 5. FEATURES_OK bit set.
                if ((change & status & VIRTIO_STATUS_FEATURES_OK) != 0) {
                    // Driver set FEATURES_OK, validate negotiated features.
                    if (!isFeatureSubsetSupported(getNegotiatedFeatures())) {
                        status &= ~VIRTIO_STATUS_FEATURES_OK;
                    } else {
                        if ((status & VIRTIO_F_RING_PACKED) != 0) {
                            throw new AssertionError("Packed queues not implemented");
                        } else {
                            for (int i = 0; i < queues.length; i++) {
                                queues[i] = new SplitVirtqueue();
                            }
                        }

                        handleFeaturesNegotiated();
                    }
                }

                // 6. FEATURES_OK read back to verify.
                // 7. Queue setup and config writes.
                // 8. DRIVER_OK bit set.
                if ((change & status & VIRTIO_STATUS_DRIVER_OK) != 0) {
                    handleDeviceSetupComplete();
                }

                if ((change & status & VIRTIO_STATUS_FAILED) != 0) {
                    handleDeviceSetupFailed();
                }

                if (value == 0) {
                    reset();
                }
                break;
            }

            case VIRTIO_MMIO_QUEUE_DESC_LOW: {
                queues[queueSel].desc = (queues[queueSel].desc & ~0xFFFFFFFFL) | ((long) value & 0xFFFFFFFFL);
                break;
            }
            case VIRTIO_MMIO_QUEUE_DESC_HIGH: {
                queues[queueSel].desc = (queues[queueSel].desc & 0xFFFFFFFFL) | ((long) value << 32);
                break;
            }

            case VIRTIO_MMIO_QUEUE_DRIVER_LOW: {
                queues[queueSel].driver = (queues[queueSel].driver & ~0xFFFFFFFFL) | ((long) value & 0xFFFFFFFFL);
                break;
            }
            case VIRTIO_MMIO_QUEUE_DRIVER_HIGH: {
                queues[queueSel].driver = (queues[queueSel].driver & 0xFFFFFFFFL) | ((long) value << 32);
                break;
            }

            case VIRTIO_MMIO_QUEUE_DEVICE_LOW: {
                queues[queueSel].device = (queues[queueSel].device & ~0xFFFFFFFFL) | ((long) value & 0xFFFFFFFFL);
                break;
            }
            case VIRTIO_MMIO_QUEUE_DEVICE_HIGH: {
                queues[queueSel].device = (queues[queueSel].device & 0xFFFFFFFFL) | ((long) value << 32);
                break;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // InterruptSource

    @Override
    public final Iterable<Interrupt> getInterrupts() {
        return Collections.singletonList(interrupt);
    }

    ///////////////////////////////////////////////////////////////////
    // Resettable

    @Override
    public void reset() {
        status = 0;
        interruptStatus = 0; // 4.2.2.1: Must clear on reset.
        deviceFeaturesSel = 0;
        driverFeatures = 0;
        driverFeaturesSel = 0;
        queueSel = 0;
        configGeneration = 0;
        Arrays.fill(queues, null);
        interrupt.lowerInterrupt();

        initializeConfig();
    }

    ///////////////////////////////////////////////////////////////////

    private void updateInterrupts() {
        // 2.1.2: Do not send notifications before DRIVER_OK.
        if (interruptStatus == 0 || (status & VIRTIO_STATUS_DRIVER_OK) == 0) {
            interrupt.lowerInterrupt();
        } else {
            interrupt.raiseInterrupt();
        }
    }

    /**
     * Abstract representation of a Virtqueue.
     * <p>
     * Actual implementations are {@link SplitVirtqueue}s and <em>Packed Virtqueues</em>.
     */
    private static abstract class AbstractVirtqueue implements VirtqueueIterator {
        int ready;
        int num = VIRTQ_MAX_QUEUE_SIZE; // Guaranteed to be a power of two.
        long desc; // Descriptor Area - used for describing buffers.
        long driver; // Driver Area - extra data supplied by driver to the device.
        long device; // Device Area - extra data supplied by device to driver.

        boolean dispatchQueueNotifications = true; // call handleQueueNotification on device?

        void reset() {
            ready = 0; // 4.2.2.1: Must set to zero on reset.
            num = VIRTQ_MAX_QUEUE_SIZE;
            desc = 0;
            driver = 0;
            device = 0;
        }

        abstract void handleQueueNotification(final int queueIndex) throws VirtIODeviceException, MemoryAccessException;
    }

    /**
     * Implementation of Split Virtqueues as defined in chapter 2.6 of the VirtIO spec.
     */
    private final class SplitVirtqueue extends AbstractVirtqueue {
        private static final int VIRTQ_DESC_TABLE_STRIDE = 16;
        private static final int VIRTQ_DESC_ADDR = 0;
        private static final int VIRTQ_DESC_LEN = 8;
        private static final int VIRTQ_DESC_FLAGS = 12;
        private static final int VIRTQ_DESC_NEXT = 14;

        private static final int VIRTQ_AVAIL_FLAGS = 0;
        private static final int VIRTQ_AVAIL_IDX = 2;
        private static final int VIRTQ_AVAIL_RING = 4;
        private static final int VIRTQ_AVAILABLE_RING_STRIDE = 2;

        private static final int VIRTQ_USED_FLAGS = 0;
        private static final int VIRTQ_USED_IDX = 2;
        private static final int VIRTQ_USED_RING = 4;
        private static final int VIRTQ_USED_RING_STRIDE = 8;
        private static final int VIRTQ_USED_RING_ELEM_ID = 0;
        private static final int VIRTQ_USED_RING_ELEM_LEN = 4;

        private static final int VIRTQ_DESC_F_NEXT = 1;
        private static final int VIRTQ_DESC_F_WRITE = 2;
        private static final int VIRTQ_DESC_F_INDIRECT = 4;

        /**
         * This is where we last stopped iterating the available descriptors ring buffer.
         */
        short lastAvailIdx;

        @Override
        void reset() {
            super.reset();
            lastAvailIdx = 0;
        }

        @Override
        public boolean hasNext() throws MemoryAccessException {
            return ready != 0 && lastAvailIdx != getAvailIdx();
        }

        @Override
        public DescriptorChain next() throws VirtIODeviceException, MemoryAccessException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return new DescriptorChainImpl(getAvailRing(lastAvailIdx++));
        }

        @Override
        void handleQueueNotification(final int queueIndex) throws VirtIODeviceException, MemoryAccessException {
            if (ready == 0 || !dispatchQueueNotifications) {
                return;
            }

            if (hasNext()) {
                AbstractVirtIODevice.this.handleQueueNotification(queueIndex);
            }
        }

        // The following methods provide access to a struct with the following layout:
        // struct virtq_desc {
        //     le64 addr;
        //     le32 len;
        //     le16 flags;
        //     le16 next;
        // };
        // virtq_desc is the structure of which we expect an array at the physical address the `desc` field points at.

        int getDescAddress(final int i) throws MemoryAccessException {
            return memoryMap.load(descIndexToAddress(i) + VIRTQ_DESC_ADDR, Sizes.SIZE_64_LOG2);
        }

        int getDescLength(final int i) throws MemoryAccessException {
            return memoryMap.load(descIndexToAddress(i) + VIRTQ_DESC_LEN, Sizes.SIZE_32_LOG2);
        }

        int getDescFlags(final int i) throws MemoryAccessException {
            return memoryMap.load(descIndexToAddress(i) + VIRTQ_DESC_FLAGS, Sizes.SIZE_16_LOG2);
        }

        int getDescNext(final int i) throws MemoryAccessException {
            return memoryMap.load(descIndexToAddress(i) + VIRTQ_DESC_NEXT, Sizes.SIZE_16_LOG2);
        }

        int descIndexToAddress(final int i) {
            return (int) desc + i * VIRTQ_DESC_TABLE_STRIDE;
        }

        // The following methods provide access to a struct with the following layout:
        // struct virtq_avail {
        //     le16 flags;
        //     le16 idx;
        //     le16 ring[];
        //     /* Only if VIRTIO_F_EVENT_IDX: le16 used_event; */
        // };
        // virtq_avail is the structure expected at the physical address the `driver` field points at.

        int getAvailFlags() throws MemoryAccessException {
            return memoryMap.load((int) driver + VIRTQ_AVAIL_FLAGS, Sizes.SIZE_16_LOG2);
        }

        int getAvailIdx() throws MemoryAccessException {
            return memoryMap.load((int) driver + VIRTQ_AVAIL_IDX, Sizes.SIZE_16_LOG2);
        }

        int getAvailRing(final int i) throws MemoryAccessException {
            final int address = (int) driver + VIRTQ_AVAIL_RING + toWrappedRingIndex(i) * VIRTQ_AVAILABLE_RING_STRIDE;
            return memoryMap.load(address, Sizes.SIZE_16_LOG2);
        }

        int getAvailUsedEvent() throws MemoryAccessException {
            return memoryMap.load((int) driver + VIRTQ_AVAIL_RING + num * VIRTQ_AVAILABLE_RING_STRIDE, Sizes.SIZE_16_LOG2);
        }

        // The following methods provide access to a struct with the following layout:
        // struct virtq_used {
        //     le16 flags;
        //     le16 idx;
        //     struct virtq_used_elem ring[];
        //     /* Only if VIRTIO_F_EVENT_IDX: le16 avail_event; */
        // };
        // struct virtq_used_elem {
        //     le32 id;
        //     le32 len;
        // };
        // virtq_used is the structure expected at the physical address the `device` field points at.

        void setUsedFlags(final int value) throws MemoryAccessException {
            memoryMap.store((int) device + VIRTQ_USED_FLAGS, value, Sizes.SIZE_16_LOG2);
        }

        short getUsedIdx() throws MemoryAccessException {
            return (short) memoryMap.load((int) device + VIRTQ_USED_IDX, Sizes.SIZE_16_LOG2);
        }

        void setUsedIdx(final short value) throws MemoryAccessException {
            memoryMap.store((int) device + VIRTQ_USED_IDX, value, Sizes.SIZE_16_LOG2);
        }

        void setUsedRing(final int i, final int id, final int len) throws MemoryAccessException {
            final int address = (int) device + VIRTQ_USED_RING + toWrappedRingIndex(i) * VIRTQ_USED_RING_STRIDE;
            memoryMap.store(address + VIRTQ_USED_RING_ELEM_ID, id, Sizes.SIZE_32_LOG2);
            memoryMap.store(address + VIRTQ_USED_RING_ELEM_LEN, len, Sizes.SIZE_32_LOG2);
        }

        void setUsedAvailEvent(final int value) throws MemoryAccessException {
            memoryMap.store((int) device + VIRTQ_USED_RING + num * VIRTQ_USED_RING_STRIDE, value, Sizes.SIZE_16_LOG2);
        }

        // Utility methods.

        int toWrappedRingIndex(final int index) {
            return index & (num - 1);
        }

        final class DescriptorChainImpl implements DescriptorChain {
            final int headDescIdx;
            final int readableByteCount;
            final int writableByteCount;
            int readByteCount;
            int writtenByteCount;
            boolean isUsed;

            int descIdx;
            int address;
            int length;
            int position;
            int chainLength = 1;

            DescriptorChainImpl(final int headDescIdx) throws VirtIODeviceException, MemoryAccessException {
                this.headDescIdx = headDescIdx;

                // Compute readable and writable byte counts.
                int readableByteCount = 0, writableByteCount = 0;
                int descIdx = headDescIdx;
                int descFlags = getDescFlags(descIdx);
                int descLength = getDescLength(descIdx);
                int chainLength = 1;

                // Readable bytes preceding writable bytes.
                boolean hasDesc = true;
                for (; ; ) {
                    if ((descFlags & VIRTQ_DESC_F_WRITE) != 0) {
                        break;
                    }

                    readableByteCount += descLength;

                    if ((descFlags & VIRTQ_DESC_F_NEXT) == 0) {
                        hasDesc = false;
                        break;
                    }

                    if (chainLength >= VIRTQ_MAX_CHAIN_LENGTH) {
                        // Chain too long. Possibly a loop.
                        error(); // Set error state immediately in case this gets caught by implementation code.
                        throw new VirtIODeviceException();
                    }

                    descIdx = getDescNext(descIdx);
                    descFlags = getDescFlags(descIdx);
                    descLength = getDescLength(descIdx);

                    chainLength++;
                }

                if (hasDesc) {
                    // Writable bytes, at this point we must no longer encounter any read-only descriptors.
                    for (; ; ) {
                        if ((descFlags & VIRTQ_DESC_F_WRITE) == 0) {
                            // 2.7.17: read-only descriptors *must* precede write-only descriptors.
                            error(); // Set error state immediately in case this gets caught by implementation code.
                            throw new VirtIODeviceException();
                        }

                        writableByteCount += descLength;

                        if ((descFlags & VIRTQ_DESC_F_NEXT) == 0) {
                            break;
                        }

                        if (chainLength >= VIRTQ_MAX_CHAIN_LENGTH) {
                            // Chain too long. Possibly a loop.
                            error(); // Set error state immediately in case this gets caught by implementation code.
                            throw new VirtIODeviceException();
                        }

                        descIdx = getDescNext(descIdx);
                        descFlags = getDescFlags(descIdx);
                        descLength = getDescLength(descIdx);

                        chainLength++;
                    }
                }

                this.readableByteCount = readableByteCount;
                this.writableByteCount = writableByteCount;

                setDescriptor(headDescIdx);
            }

            @Override
            public void use() throws MemoryAccessException {
                if (isUsed) {
                    return;
                }
                isUsed = true;

                // 2.6.8.2: set len prior to updating used idx.
                short index = getUsedIdx();
                setUsedRing(index, headDescIdx, writtenByteCount);
                index++; // Overflow by design.
                setUsedIdx(index);

                // 2.6.7: Used Buffer Notification Suppression
                final boolean sendNotification;
                if ((getNegotiatedFeatures() & VIRTIO_F_RING_EVENT_IDX) == 0) {
                    final int flags = getAvailFlags();
                    sendNotification = flags == 0;
                } else {
                    final int usedEvent = getAvailUsedEvent();
                    sendNotification = index == usedEvent;
                }

                if (sendNotification) {
                    interruptStatus |= VIRTIO_IRQ_USED_BUFFER_MASK;
                    updateInterrupts();
                }
            }

            @Override
            public int readableBytes() {
                if (isUsed) return 0;
                assert readByteCount <= readableByteCount;
                return readableByteCount - readByteCount;
            }

            @Override
            public int writableBytes() {
                if (isUsed) return 0;
                assert writtenByteCount <= writableByteCount;
                return writableByteCount - writtenByteCount;
            }

            @Override
            public byte get() throws VirtIODeviceException, MemoryAccessException {
                if (isUsed) {
                    throw new IllegalStateException();
                }
                if (readableBytes() <= 0) {
                    throw new IndexOutOfBoundsException();
                }

                assert position < length;
                final int value = memoryMap.load(address + position, Sizes.SIZE_8_LOG2);
                readByteCount++;
                position++;
                if (position >= length) {
                    nextDescriptor();
                }

                return (byte) value;
            }

            @Override
            public void get(final byte[] dst, final int offset, final int length) throws VirtIODeviceException, MemoryAccessException {
                if (isUsed) {
                    throw new IllegalStateException();
                }
                if (length > readableBytes()) {
                    throw new IndexOutOfBoundsException();
                }

                // TODO Optimize
                for (int i = 0; i < length; i++) {
                    dst[offset + i] = get();
                }
            }

            @Override
            public void get(final ByteBuffer dst) throws VirtIODeviceException, MemoryAccessException {
                if (isUsed) {
                    throw new IllegalStateException();
                }
                if (dst.remaining() > readableBytes()) {
                    throw new IndexOutOfBoundsException();
                }

                // TODO Optimize
                while (dst.hasRemaining()) {
                    dst.put(get());
                }
            }

            @Override
            public void put(final byte value) throws VirtIODeviceException, MemoryAccessException {
                if (isUsed) {
                    throw new IllegalStateException();
                }
                if (readableBytes() > 0) {
                    throw new IllegalStateException();
                }
                if (writableBytes() <= 0) {
                    throw new IndexOutOfBoundsException();
                }

                assert position < length;
                memoryMap.store(address + position, value, Sizes.SIZE_8_LOG2);
                writtenByteCount++;
                position++;
                if (position >= length) {
                    nextDescriptor();
                }
            }

            @Override
            public void put(final byte[] src, final int offset, final int length) throws VirtIODeviceException, MemoryAccessException {
                if (isUsed) {
                    throw new IllegalStateException();
                }
                if (readableBytes() > 0) {
                    throw new IllegalStateException();
                }
                if (length > writableBytes()) {
                    throw new IndexOutOfBoundsException();
                }

                // TODO Optimize
                for (int i = 0; i < length; i++) {
                    put(src[offset + i]);
                }
            }

            @Override
            public void put(final ByteBuffer src) throws VirtIODeviceException, MemoryAccessException {
                if (isUsed) {
                    throw new IllegalStateException();
                }
                if (readableBytes() > 0) {
                    throw new IllegalStateException();
                }
                if (src.remaining() > writableBytes()) {
                    throw new IndexOutOfBoundsException();
                }

                // TODO Optimize
                while (src.hasRemaining()) {
                    put(src.get());
                }
            }

            void setDescriptor(final int descIdx) throws MemoryAccessException {
                this.descIdx = descIdx;
                address = getDescAddress(descIdx);
                length = getDescLength(descIdx);
                position = 0;
            }

            void nextDescriptor() throws VirtIODeviceException, MemoryAccessException {
                if (position < length) {
                    throw new IllegalStateException("Current descriptor must be used up before advancing to the next.");
                }

                if ((getDescFlags(descIdx) & VIRTQ_DESC_F_NEXT) == 0) {
                    return; // End of chain reached, nothing left to do.
                }

                // We checked this when computing the length, but we have to prepare for the worst
                // since the driver may be malicious and change our descriptors while we're iterating.
                if (chainLength >= VIRTIO_MMIO_QUEUE_NUM_MAX) {
                    error(); // Set error state immediately in case this gets caught by implementation code.
                    throw new VirtIODeviceException();
                } else {
                    setDescriptor(getDescNext(descIdx));
                    chainLength++;
                }

                // Again, we checked this before, but we don't trust the driver. If we already had a
                // write-only buffer then we must not see any read-only buffers.
                if ((getDescFlags(descIdx) & VIRTQ_DESC_F_WRITE) == 0 && writtenByteCount > 0) {
                    error(); // Set error state immediately in case this gets caught by implementation code.
                    throw new VirtIODeviceException();
                }
            }
        }
    }
}
