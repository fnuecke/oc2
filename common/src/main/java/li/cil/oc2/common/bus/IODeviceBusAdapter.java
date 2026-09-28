/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.io.IOInvocation;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.common.util.ThrottledLogger;
import li.cil.sedna.api.Interrupt;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.InterruptSource;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.device.bus.DeviceClass;
import li.cil.sedna.api.device.bus.DeviceDescription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.IntPredicate;

public final class IODeviceBusAdapter implements MemoryMappedDevice, InterruptSource {
    private static final Logger LOGGER = LogManager.getLogger(IODeviceBusAdapter.class);
    private static final ThrottledLogger THROTTLED_LOGGER = new ThrottledLogger(LOGGER, Duration.ofMinutes(1));

    public static final int LENGTH = 8;
    public static final DeviceClass DEVICE_CLASS = new DeviceClass(DeviceClass.THIRD_PARTY_BASE);

    private static final int REG_SELECT = 0;
    private static final int REG_FUNCTION = 1;
    private static final int REG_DATA = 2;
    private static final int REG_STATUS = 3;
    private static final int REG_EVENT_CONTROL = 4;
    private static final int REG_EVENT_DATA = 5;

    private static final int STATUS_BUSY = 0b0000_0001;
    private static final int STATUS_DATA_AVAILABLE = 0b0000_0010;
    private static final int STATUS_ERROR = 0b1000_0000;

    private static final int CONTROL_ABORT = 0x00;

    private static final int ERROR_NONE = 0x00;
    private static final int ERROR_NO_SUCH_DEVICE = 0x01;
    private static final int ERROR_NO_SUCH_FUNCTION = 0x02;
    private static final int ERROR_INVALID_ARGUMENTS = 0x05;
    private static final int ERROR_INTERNAL = 0x06;

    private static final int STATE_IDLE = 0;
    private static final int STATE_ARGUMENTS = 1;
    private static final int STATE_RUNNING = 2;
    private static final int STATE_DONE = 3;

    private static final int BUFFER_SIZE = IOCallback.MAX_DATA_SIZE;

    // --------------------------------------------------------------------- //
    // Worker thread owned

    @Serialized
    private int selected;
    @Serialized
    private UUID selectedIdentifier; // track exact device to detect shifts across bus scans
    @Serialized
    private int state;
    @Serialized
    private int functionCode;
    @Serialized
    private int errorCode;
    @Serialized
    private final byte[] arguments = new byte[BUFFER_SIZE];
    @Serialized
    private int argumentCount;
    @Serialized
    private int resultCursor;
    @Serialized
    private boolean isAbandoned;

    @Serialized
    private int sequence;
    @Serialized
    private volatile int requestSequence;
    @Serialized
    private UUID pendingDeviceId;
    @Serialized
    private int pendingFunctionCode;

    // --------------------------------------------------------------------- //
    // Shared ownership; server thread while handling an invocation

    @Serialized
    private final byte[] results = new byte[BUFFER_SIZE];
    @Serialized
    private int resultCount;
    @Serialized
    private int completionError;

    @Serialized
    private volatile int completedSequence;

    // --------------------------------------------------------------------- //

    private final transient IntPredicate consumeEnergy;
    private final transient IODeviceRegistry registry = new IODeviceRegistry(this::sendEvent);
    @Serialized
    private final IOEventQueue events = new IOEventQueue(registry);

    // --------------------------------------------------------------------- //

    public IODeviceBusAdapter(final IntPredicate consumeEnergy) {
        this.consumeEnergy = consumeEnergy;
    }

    // --------------------------------------------------------------------- //

    public List<DeviceDescription> getDescriptions() {
        return registry.descriptions();
    }

    public Interrupt getInterrupt() {
        return events.getInterrupt();
    }

    public void mountDevices() {
        registry.mountDevices();
    }

    public void unmountDevices() {
        registry.unmountDevices();
    }

    public void reset() {
        resetTransaction();
        selected = 0;
        selectedIdentifier = null;
        sequence = 0;
        requestSequence = 0;
        completedSequence = 0;
        completionError = ERROR_NONE;
        pendingDeviceId = null;
        pendingFunctionCode = 0;
        events.reset();
    }

    public void rebuild(final DeviceBusController controller) {
        registry.rebuild(controller);
        events.dropEventsOfDroppedDevices();
    }

    public void step() {
        events.step();
    }

    public void tick() {
        final int request = requestSequence; // acquire; publishes the staged invocation
        if (request == completedSequence) {
            return;
        }

        resultCount = 0;

        final IODevice device = registry.byKey(pendingDeviceId);
        final IOMethod function = device != null ? findFunction(device, pendingFunctionCode) : null;
        if (device == null) {
            completionError = ERROR_NO_SUCH_DEVICE;
        } else if (function == null) {
            completionError = ERROR_NO_SUCH_FUNCTION;
        } else {
            completionError = invoke(function, consumeEnergy);
        }

        completedSequence = request; // release; publishes the results
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public int getSupportedSizes() {
        return 1 << Sizes.SIZE_8_LOG2;
    }

    @Override
    public long load(final int offset, final int sizeLog2) {
        observeCompletion();
        return switch (offset) {
            case REG_SELECT -> selected;
            case REG_DATA -> readData();
            case REG_STATUS -> readStatus();
            case REG_EVENT_CONTROL -> events.readControl();
            case REG_EVENT_DATA -> events.readData();
            default -> 0;
        };
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) {
        final int data = (int) (value & 0xFF);
        if (offset == REG_EVENT_CONTROL) {
            events.writeControl(data);
            return;
        }

        observeCompletion();

        if (state == STATE_RUNNING && offset != REG_STATUS) {
            return;
        }

        switch (offset) {
            case REG_SELECT -> select(data);
            case REG_FUNCTION -> begin(data);
            case REG_DATA -> writeArgument(data);
            case REG_STATUS -> control(data);
            default -> {
            }
        }
    }

    @Override
    public Iterable<Interrupt> getInterrupts() {
        return List.of(events.getInterrupt());
    }

    // --------------------------------------------------------------------- //

    private void observeCompletion() {
        if (state != STATE_RUNNING || completedSequence != requestSequence) { // acquire
            return;
        }

        if (isAbandoned) {
            resetTransaction();
            return;
        }

        errorCode = completionError;
        resultCursor = 0;
        state = STATE_DONE;
    }

    private int readData() {
        if (errorCode != ERROR_NONE) {
            return errorCode;
        }
        if (state == STATE_DONE && resultCursor < resultCount) {
            return results[resultCursor++] & 0xFF;
        }
        return 0;
    }

    private int readStatus() {
        int status = 0;
        if (state == STATE_RUNNING) {
            status |= STATUS_BUSY;
        }
        if (errorCode != ERROR_NONE) {
            status |= STATUS_ERROR;
        } else if (state == STATE_DONE && resultCursor < resultCount) {
            status |= STATUS_DATA_AVAILABLE;
        }
        return status;
    }

    private void select(final int index) {
        resetTransaction();
        selected = index;
        selectedIdentifier = registry.keyAt(index);
    }

    private void begin(final int code) {
        resetTransaction();

        if (code > IOCallback.MAX_CODE) {
            errorCode = ERROR_NO_SUCH_FUNCTION;
            return;
        }

        functionCode = code;
        state = STATE_ARGUMENTS;
    }

    private void writeArgument(final int value) {
        if (state != STATE_ARGUMENTS) {
            errorCode = ERROR_INVALID_ARGUMENTS;
            return;
        }
        if (argumentCount == BUFFER_SIZE) {
            resetTransaction();
            errorCode = ERROR_INVALID_ARGUMENTS;
            return;
        }

        arguments[argumentCount++] = (byte) value;
    }

    private void control(final int value) {
        if (value == CONTROL_ABORT) {
            abort();
        } else {
            execute();
        }
    }

    private void abort() {
        if (state == STATE_RUNNING) {
            isAbandoned = true;
            errorCode = ERROR_NONE;
            return;
        }

        resetTransaction();
    }

    private void execute() {
        if (state == STATE_RUNNING) {
            return;
        }
        if (state != STATE_ARGUMENTS) {
            errorCode = ERROR_NO_SUCH_FUNCTION;
            return;
        }

        final IODevice device = selectedIdentifier != null ? registry.byKey(selectedIdentifier) : null;
        if (device == null) {
            resetTransaction();
            errorCode = ERROR_NO_SUCH_DEVICE;
            return;
        }

        final IOMethod function = findFunction(device, functionCode);
        if (function == null) {
            resetTransaction();
            errorCode = ERROR_NO_SUCH_FUNCTION;
            return;
        }

        if (function.isSynchronized()) {
            pendingDeviceId = selectedIdentifier;
            pendingFunctionCode = functionCode;
            isAbandoned = false;
            state = STATE_RUNNING;
            sequence++;
            requestSequence = sequence; // release; publishes the arguments
        } else {
            resultCount = 0;
            errorCode = invoke(function, null);
            resultCursor = 0;
            state = STATE_DONE;
        }
    }

    private void resetTransaction() {
        state = STATE_IDLE;
        functionCode = 0;
        errorCode = ERROR_NONE;
        argumentCount = 0;
        resultCursor = 0;
        resultCount = 0;
        isAbandoned = false;
    }

    private int invoke(final IOMethod function, @Nullable final IntPredicate consumeEnergy) {
        try {
            function.invoke(new Invocation(new ByteArrayInputStream(arguments, 0, argumentCount), new ResultStream(), consumeEnergy));
            return ERROR_NONE;
        } catch (final EOFException | IllegalArgumentException | IllegalStateException e) {
            resultCount = 0;
            return ERROR_INVALID_ARGUMENTS;
        } catch (final Throwable e) {
            resultCount = 0;
            THROTTLED_LOGGER.error("Device function invocation failed.", e);
            return ERROR_INTERNAL;
        }
    }

    private boolean sendEvent(final UUID key, final int code, final int value) {
        return events.enqueue(key, code, value);
    }

    @Nullable
    private static IOMethod findFunction(final IODevice device, final int code) {
        for (final IOMethod function : device.getIOMethods()) {
            if (function.getCode() == code) {
                return function;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------- //

    private record Invocation(InputStream input, OutputStream output, @Nullable IntPredicate consumeEnergy) implements IOInvocation {
        @Override
        public InputStream getInput() {
            return input;
        }

        @Override
        public OutputStream getOutput() {
            return output;
        }

        @Override
        public boolean consumeEnergy(final int amount) {
            if (consumeEnergy == null) {
                throw new IllegalStateException("energy can only be consumed in synchronized calls");
            }
            return consumeEnergy.test(amount);
        }
    }

    private final class ResultStream extends OutputStream {
        @Override
        public void write(final int b) throws IOException {
            if (resultCount == BUFFER_SIZE) {
                throw new IOException("Device wrote more than " + BUFFER_SIZE + " bytes of results.");
            }
            results[resultCount++] = (byte) b;
        }
    }
}
