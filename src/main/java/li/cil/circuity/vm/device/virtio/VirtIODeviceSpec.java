package li.cil.circuity.vm.device.virtio;

/**
 * Device specifications for a VirtIO device.
 * <p>
 * Create instances of this type using a builder obtained by calling {@link #builder(int)}.
 */
public final class VirtIODeviceSpec {
    private static final int MAX_CONFIG_SPACE_SIZE = 256;
    private static final int MAX_VIRTQUEUE_COUNT = 16;

    public final int deviceId;
    public final int vendorId;
    public final long features;
    public final int configSpaceSizeInBytes;
    public final int virtQueueCount;

    VirtIODeviceSpec(final int deviceId,
                     final int vendorId,
                     final long features,
                     final int configSpaceSizeInBytes,
                     final int virtQueueCount) {
        if (configSpaceSizeInBytes < 0 || configSpaceSizeInBytes > MAX_CONFIG_SPACE_SIZE) {
            throw new IndexOutOfBoundsException();
        }
        if (virtQueueCount < 0 || virtQueueCount > MAX_VIRTQUEUE_COUNT) {
            throw new IndexOutOfBoundsException();
        }

        this.deviceId = deviceId;
        this.vendorId = vendorId;
        this.features = features | AbstractVirtIODevice.VIRTIO_F_VERSION_1;
        this.configSpaceSizeInBytes = configSpaceSizeInBytes;
        this.virtQueueCount = virtQueueCount;
    }

    /**
     * Creates a new spec build for setting up specs using method chaining.
     *
     * @param deviceId the device id of the VirtIO device this spec is for.
     * @return a new spec builder.
     */
    public static Builder builder(final int deviceId) {
        return new Builder(deviceId);
    }

    /**
     * Builder for {@link VirtIODeviceSpec} instances using method chaining.
     */
    public static final class Builder {
        private final int deviceId;
        private int vendorId = AbstractVirtIODevice.VIRTIO_VENDOR_ID_GENERIC;
        private long features;
        private int configSpaceSizeInBytes;
        private int virtQueueCount;

        /**
         * Configures the vendor id for devices with this device spec.
         * <p>
         * This defaults to the generic/experimental vendor id.
         *
         * @param value the vendor id to use.
         * @return this builder for method chaining.
         */
        public Builder vendorId(final int value) {
            this.vendorId = value;
            return this;
        }

        /**
         * Configures the supported feature set for devices with this device spec.
         *
         * @param value the bitmask defining the supported features.
         * @return this builder for method chaining.
         */
        public Builder features(final long value) {
            this.features = value;
            return this;
        }

        /**
         * Configures the size of the config space for devices with this device spec.
         *
         * @param sizeInBytes the size of the config space in bytes.
         * @return this builder for method chaining.
         */
        public Builder configSpaceSize(final int sizeInBytes) {
            if (sizeInBytes < 0 || sizeInBytes > MAX_CONFIG_SPACE_SIZE) {
                throw new IndexOutOfBoundsException();
            }
            this.configSpaceSizeInBytes = sizeInBytes;
            return this;
        }

        /**
         * Configures the number for {@link VirtqueueIterator}s
         * devices with this spec use.
         *
         * @param value the number of queues.
         * @return this builder for method chaining.
         */
        public Builder queueCount(final int value) {
            if (value < 0 || value > MAX_VIRTQUEUE_COUNT) {
                throw new IndexOutOfBoundsException();
            }
            virtQueueCount = value;
            return this;
        }

        /**
         * Finishes construction of a {@link VirtIODeviceSpec} and returns it.
         *
         * @return the spec configured using this builder.
         */
        public VirtIODeviceSpec build() {
            return new VirtIODeviceSpec(deviceId, vendorId, features, configSpaceSizeInBytes, virtQueueCount);
        }

        private Builder(final int deviceId) {
            this.deviceId = deviceId;
        }
    }
}
