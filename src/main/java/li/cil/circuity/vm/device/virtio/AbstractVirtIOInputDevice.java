package li.cil.circuity.vm.device.virtio;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.vm.evdev.EvdevEvents;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractVirtIOInputDevice extends AbstractVirtIODevice {
    protected static final int VIRTIO_INPUT_CFG_SELECT_UNSET = 0x00;
    protected static final int VIRTIO_INPUT_CFG_SELECT_ID_NAME = 0x01;
    protected static final int VIRTIO_INPUT_CFG_SELECT_ID_SERIAL = 0x02;
    protected static final int VIRTIO_INPUT_CFG_SELECT_ID_DEVIDS = 0x03;
    protected static final int VIRTIO_INPUT_CFG_SELECT_PROP_BITS = 0x10;
    protected static final int VIRTIO_INPUT_CFG_SELECT_EV_BITS = 0x11;
    protected static final int VIRTIO_INPUT_CFG_SELECT_ABS_INFO = 0x12;

    // Config looks like this:
    // struct virtio_input_config {
    //     u8    select;
    //     u8    subsel;
    //     u8    size;
    //     u8    reserved[5];
    //     union {
    //       char string[128];
    //       u8   bitmap[128];
    //       struct virtio_input_absinfo abs;
    //       struct virtio_input_devids ids;
    //     } u;
    // };
    private static final int VIRTIO_INPUT_CFG_SELECT_OFFSET = 0x0;
    private static final int VIRTIO_INPUT_CFG_SUBSEL_OFFSET = 0x1;
    private static final int VIRTIO_INPUT_CFG_SIZE_OFFSET = 0x2;
    private static final int VIRTIO_INPUT_CFG_UNION_OFFSET = 0x8;
    private static final int VIRTIO_INPUT_CFG_UNION_SIZE = 128;

    private static final int VIRTQ_EVENT = 0;
    private static final int VIRTQ_STATUS = 1;

    private static final ThreadLocal<ByteBuffer> eventBuffer = new ThreadLocal<>();

    // This holds the union with info in the config struct. We generate this whenever the state
    // changes and keep it for more efficient access (and fewer headaches).
    private final ByteBuffer config = ByteBuffer.allocate(VIRTIO_INPUT_CFG_UNION_SIZE);
    private boolean configDirty = true;

    private DescriptorChain event;

    protected AbstractVirtIOInputDevice(final MemoryMap memoryMap) {
        super(memoryMap, VirtIODeviceSpec.builder(VirtIODeviceType.VIRTIO_DEVICE_ID_INPUT_DEVICE)
                // We only physically use 2 bytes of config space, but this is used for getLength(), too.
                .configSpaceSize(256)
                .queueCount(2)
                .build());
    }

    protected abstract void generateConfigUnion(final int select, final int subsel, final ByteBuffer config);

    protected void handleStatus(final int type, final int code, final int value) {
    }

    protected final void putEvent(final int type, final int code, final int value) {
        if ((getStatus() & VIRTIO_STATUS_FAILED) != 0) {
            return;
        }

        try {
            // 5.8.6.1: These buffers [in the eventq] MUST be device-writable [...]
            event = validateWriteOnlyDescriptorChain(VIRTQ_EVENT, event);
            if (event != null) {
                // 5.8.6.1: [eventq buffers] MUST be at least the size of struct virtio_input_event.
                if (event.writableBytes() < 8) {
                    error();
                    return;
                }

                // VirtIO Input Events look like this:
                // struct virtio_input_event {
                //     le16 type;
                //     le16 code;
                //     le32 value;
                // };
                final ByteBuffer buffer = getTempBuffer();
                buffer.putShort((short) type);
                buffer.putShort((short) code);
                buffer.putInt(value);

                buffer.flip();
                event.put(buffer);
                event.use();
            }
        } catch (final VirtIODeviceException | MemoryAccessException e) {
            error();
        }
    }

    protected final void putSyn() {
        putEvent(EvdevEvents.EV_SYN, 0, 0);
    }

    @Override
    protected final int loadConfig(final int offset, final int sizeLog2) {
        if (sizeLog2 != Sizes.SIZE_8_LOG2) {
            return 0;
        }

        if (offset == VIRTIO_INPUT_CFG_SIZE_OFFSET) {
            validateConfig();
            return config.limit();
        } else if (offset >= VIRTIO_INPUT_CFG_UNION_OFFSET && offset < VIRTIO_INPUT_CFG_UNION_OFFSET + VIRTIO_INPUT_CFG_UNION_SIZE) {
            validateConfig();
            return config.get(offset - VIRTIO_INPUT_CFG_UNION_OFFSET);
        }

        return super.loadConfig(offset, sizeLog2);
    }

    @Override
    protected final void storeConfig(final int offset, final int value, final int sizeLog2) {
        if (offset > VIRTIO_INPUT_CFG_SUBSEL_OFFSET) {
            return; // Only select and subsel are writable by the driver.
        }

        super.storeConfig(offset, value, sizeLog2);
        configDirty = true;
    }

    @Override
    protected final void handleFeaturesNegotiated() {
        setQueueNotifications(VIRTQ_EVENT, false);
    }

    @Override
    protected final void handleQueueNotification(final int queueIndex) throws VirtIODeviceException, MemoryAccessException {
        if (queueIndex == VIRTQ_STATUS) {
            final VirtqueueIterator queue = getQueueIterator(queueIndex);
            if (queue != null && queue.hasNext()) {
                while (queue.hasNext()) {
                    final DescriptorChain chain = queue.next();
                    while (chain.readableBytes() >= 8) {
                        final ByteBuffer buffer = getTempBuffer();
                        chain.get(buffer);
                        buffer.flip();

                        final short type = buffer.getShort();
                        final short code = buffer.getShort();
                        final int value = buffer.getInt();
                        handleStatus(type, code, value);
                    }
                    chain.use();
                }
            }
        }
    }

    private void validateConfig() {
        if (!configDirty) {
            return;
        }

        final ByteBuffer configSpace = getConfiguration();
        final int select = configSpace.get(VIRTIO_INPUT_CFG_SELECT_OFFSET) & 0xFF;
        final int subsel = configSpace.get(VIRTIO_INPUT_CFG_SUBSEL_OFFSET) & 0xFF;

        if (select == VIRTIO_INPUT_CFG_SELECT_UNSET) {
            config.limit(0);
        } else {
            config.clear();
            generateConfigUnion(select, subsel, config);
        }

        config.position(0);

        configDirty = false;
    }

    private static ByteBuffer getTempBuffer() {
        ByteBuffer buffer = eventBuffer.get();
        if (buffer == null) {
            buffer = ByteBuffer.allocate(8);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            eventBuffer.set(buffer);
        }
        buffer.clear();
        return buffer;
    }
}
