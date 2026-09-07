/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.vm.context.VMRuntime;
import li.cil.oc2.common.util.ThrottledLogger;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.device.bus.DeviceClass;
import li.cil.sedna.api.device.bus.DeviceDescription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.*;

public final class IODeviceBusAdapter implements MemoryMappedDevice {
    private static final Logger LOGGER = LogManager.getLogger(IODeviceBusAdapter.class);
    private static final ThrottledLogger THROTTLED_LOGGER = new ThrottledLogger(LOGGER, Duration.ofMinutes(1));

    public static final int LENGTH = 4;
    public static final DeviceClass DEVICE_CLASS = new DeviceClass(DeviceClass.THIRD_PARTY_BASE);

    private static final int REG_SELECT = 0;
    private static final int REG_FUNCTION = 1;
    private static final int REG_DATA = 2;
    private static final int REG_STATUS = 3;

    private static final int STATUS_BUSY = 0b0000_0001;
    private static final int STATUS_DATA_AVAILABLE = 0b0000_0010;
    private static final int STATUS_ERROR = 0b1000_0000;

    private static final int CONTROL_ABORT = 0x00;
    private static final int CONTROL_EXECUTE = 0x01;

    private static final int ERROR_NONE = 0x00;
    private static final int ERROR_NO_SUCH_DEVICE = 0x01;
    private static final int ERROR_NO_SUCH_FUNCTION = 0x02;
    private static final int ERROR_INVALID_ARGUMENTS = 0x05;
    private static final int ERROR_INTERNAL = 0x06;

    private static final int STATE_IDLE = 0;
    private static final int STATE_ARGUMENTS = 1;
    private static final int STATE_RUNNING = 2;
    private static final int STATE_DONE = 3;

    private static final int BUFFER_SIZE = 256;
    private static final int MAX_DEVICES = 256;

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

    private final transient VMRuntime runtime;
    private transient List<Binding> bindings = List.of();
    private transient Map<UUID, Binding> bindingsById = Map.of();
    private transient List<DeviceDescription> descriptions = List.of();

    // --------------------------------------------------------------------- //

    public IODeviceBusAdapter(final VMRuntime runtime) {
        this.runtime = runtime;
    }

    // --------------------------------------------------------------------- //

    public List<DeviceDescription> getDescriptions() {
        return descriptions;
    }

    public void rebuild(final DeviceBusController controller) {
        runtime.join();

        final Map<IODevice, UUID> identifierByDevice = new HashMap<>();
        for (final Device device : controller.getDevices()) {
            if (!(device instanceof final IODevice ioDevice)) {
                continue;
            }

            if (ioDevice.getIOMethods().isEmpty()) {
                continue;
            }

            for (final UUID identifier : controller.getDeviceIdentifiers(device)) {
                identifierByDevice.merge(ioDevice, identifier, (a, b) -> a.compareTo(b) <= 0 ? a : b);
            }
        }

        final ArrayList<Binding> newBindings = new ArrayList<>(identifierByDevice.size());
        identifierByDevice.forEach((device, identifier) -> newBindings.add(new Binding(identifier, device)));

        newBindings.sort(Comparator.comparing(Binding::identifier).thenComparing(binding -> binding.device().getIOName()).thenComparing(binding -> binding.device().getClass().getName()));

        // A group's devices all share its identifier, and the guest addresses by identifier, so
        // only the first of a group can be reached. Merging them the way RPCDeviceList does is not
        // an option here: two merged devices could claim the same function code.
        for (int i = newBindings.size() - 1; i > 0; i--) {
            if (newBindings.get(i).identifier().equals(newBindings.get(i - 1).identifier())) {
                final Binding dropped = newBindings.remove(i);
                LOGGER.warn("Device [{}] shares identifier [{}] with another device providing a " + "mid-level API and is not reachable from the guest.", dropped.device().getIOName(), dropped.identifier());
            }
        }

        if (newBindings.size() > MAX_DEVICES) {
            LOGGER.warn("More than {} devices provide a mid-level API; dropping {} of them.", MAX_DEVICES, newBindings.size() - MAX_DEVICES);
            newBindings.subList(MAX_DEVICES, newBindings.size()).clear();
        }

        final Map<UUID, Binding> newBindingsById = new HashMap<>(newBindings.size());
        final ArrayList<DeviceDescription> newDescriptions = new ArrayList<>(newBindings.size());
        for (int index = 0; index < newBindings.size(); index++) {
            final Binding binding = newBindings.get(index);
            newBindingsById.put(binding.identifier(), binding);
            newDescriptions.add(new DeviceDescription(DEVICE_CLASS, binding.device().getIOName(), binding.identifier().toString(), index));
        }

        bindings = newBindings;
        bindingsById = newBindingsById;
        descriptions = newDescriptions;
    }

    public void tick() {
        final int request = requestSequence; // acquire; publishes the staged invocation
        if (request == completedSequence) {
            return;
        }

        resultCount = 0;

        final Binding binding = bindingsById.get(pendingDeviceId);
        final IOMethod function = binding != null ? findFunction(binding.device(), pendingFunctionCode) : null;
        if (binding == null) {
            completionError = ERROR_NO_SUCH_DEVICE;
        } else if (function == null) {
            completionError = ERROR_NO_SUCH_FUNCTION;
        } else {
            completionError = invoke(function);
        }

        completedSequence = request; // release; publishes the results
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
            default -> 0;
        };
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) {
        observeCompletion();

        if (state == STATE_RUNNING && offset != REG_STATUS) {
            return;
        }

        final int data = (int) (value & 0xFF);
        switch (offset) {
            case REG_SELECT -> select(data);
            case REG_FUNCTION -> begin(data);
            case REG_DATA -> writeArgument(data);
            case REG_STATUS -> control(data);
            default -> {
            }
        }
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
        selectedIdentifier = index < bindings.size() ? bindings.get(index).identifier() : null;
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
        switch (value) {
            case CONTROL_EXECUTE -> execute();
            case CONTROL_ABORT -> abort();
            default -> errorCode = ERROR_INVALID_ARGUMENTS;
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

        final Binding binding = selectedIdentifier != null ? bindingsById.get(selectedIdentifier) : null;
        if (binding == null) {
            resetTransaction();
            errorCode = ERROR_NO_SUCH_DEVICE;
            return;
        }

        final IOMethod function = findFunction(binding.device(), functionCode);
        if (function == null) {
            resetTransaction();
            errorCode = ERROR_NO_SUCH_FUNCTION;
            return;
        }

        if (function.isSynchronized()) {
            pendingDeviceId = binding.identifier();
            pendingFunctionCode = functionCode;
            isAbandoned = false;
            state = STATE_RUNNING;
            sequence++;
            requestSequence = sequence; // release; publishes the arguments
        } else {
            resultCount = 0;
            errorCode = invoke(function);
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

    private int invoke(final IOMethod function) {
        try {
            function.invoke(new ByteArrayInputStream(arguments, 0, argumentCount), new ResultStream());
            return ERROR_NONE;
        } catch (final IllegalArgumentException | IllegalStateException e) {
            resultCount = 0;
            return ERROR_INVALID_ARGUMENTS;
        } catch (final Throwable e) {
            resultCount = 0;
            THROTTLED_LOGGER.error("Device function invocation failed.", e);
            return ERROR_INTERNAL;
        }
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

    private record Binding(UUID identifier, IODevice device) {
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
