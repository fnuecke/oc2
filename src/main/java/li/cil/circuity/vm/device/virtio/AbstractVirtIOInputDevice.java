package li.cil.circuity.vm.device.virtio;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
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

    private DescriptorChain event;

    protected AbstractVirtIOInputDevice(final MemoryMap memoryMap) {
        super(memoryMap, VirtIODeviceSpec.builder(VirtIODeviceType.VIRTIO_DEVICE_ID_INPUT_DEVICE)
                .configSpaceSize(256)
                .queueCount(2)
                .build());
    }

    /**
     * Called to generate the contents union of the union of the input devices config space,
     * i.e. {@code virtio_input_config.u}. Returns the size of the generated struct, i.e.
     * {@code virtio_input_config.size}.
     *
     * @param select the current config selection
     * @param subsel the current config sub-selection.
     * @param config the union section of the config space to populate.
     * @return the size of the generated config union.
     */
    protected int generateConfigUnion(final int select, final int subsel, final ByteBuffer config) {
        return 0;
    }

    /**
     * Called when receiving status messages from the driver.
     * <p>
     * These are device specific and usually things like LED states.
     *
     * @param type  the {@code type} field of the status event.
     * @param code  the {@code code} field of the status event.
     * @param value the {@code value} field of the status event.
     */
    protected void handleStatus(final int type, final int code, final int value) {
    }

    /**
     * Calling this enqueues an event with the specified values into the event queue.
     *
     * @param type  the {@code type} field of the status event.
     * @param code  the {@code code} field of the status event.
     * @param value the {@code value} field of the status event.
     */
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

    /**
     * Calling this enqueues an {@link EvdevEvents#EV_SYN} event into the event queue.
     * <p>
     * These events are used to separate different events and should be called to finish
     * and event, e.g. after a key press or after writing all axis data.
     */
    protected final void putSyn() {
        putEvent(EvdevEvents.EV_SYN, 0, 0);
    }

    @Override
    protected final void storeConfig(final int offset, final int value, final int sizeLog2) {
        if (offset > VIRTIO_INPUT_CFG_SUBSEL_OFFSET) {
            return; // Only select and subsel are writable by the driver.
        }

        super.storeConfig(offset, value, sizeLog2);

        generateConfig();
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

    private void generateConfig() {
        final ByteBuffer config = getConfiguration();
        final int select = config.get(VIRTIO_INPUT_CFG_SELECT_OFFSET) & 0xFF;
        final int subsel = config.get(VIRTIO_INPUT_CFG_SUBSEL_OFFSET) & 0xFF;

        if (select == VIRTIO_INPUT_CFG_SELECT_UNSET) {
            config.put(VIRTIO_INPUT_CFG_SIZE_OFFSET, (byte) 0);
        } else {
            config.limit(VIRTIO_INPUT_CFG_UNION_OFFSET + VIRTIO_INPUT_CFG_UNION_SIZE);
            config.position(VIRTIO_INPUT_CFG_UNION_OFFSET);
            final ByteBuffer union = config.slice();
            config.clear();
            final int size = generateConfigUnion(select, subsel, union);
            config.put(VIRTIO_INPUT_CFG_SIZE_OFFSET, (byte) size);
        }
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
