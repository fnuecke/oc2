package li.cil.oc2.common.bus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceInterface;
import li.cil.oc2.api.bus.device.DeviceMethod;
import li.cil.oc2.api.bus.device.DeviceMethodParameter;
import li.cil.oc2.common.device.DeviceMethodParameterTypeAdapters;
import li.cil.oc2.serialization.serializers.DeviceJsonSerializer;
import li.cil.oc2.serialization.serializers.DeviceMethodJsonSerializer;
import li.cil.oc2.serialization.serializers.MessageJsonDeserializer;
import li.cil.oc2.serialization.serializers.MethodInvocationJsonDeserializer;
import li.cil.sedna.api.device.Steppable;
import li.cil.sedna.api.device.serial.SerialDevice;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RPCAdapter implements Steppable {
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 4 * 1024;
    private static final byte[] MESSAGE_DELIMITER = "\0".getBytes();

    public static final String ERROR_MESSAGE_TOO_LARGE = "message too large";
    public static final String ERROR_UNKNOWN_MESSAGE_TYPE = "unknown message type";
    public static final String ERROR_UNKNOWN_DEVICE = "unknown device";
    public static final String ERROR_UNKNOWN_METHOD = "unknown method";
    public static final String ERROR_INVALID_PARAMETER_SIGNATURE = "invalid parameter signature";

    private final DeviceBusController controller;

    private final SerialDevice serialDevice;
    private final Gson gson;

    @Serialized private final ByteBuffer transmitBuffer; // for data written to device by VM
    @Serialized private ByteBuffer receiveBuffer; // for data written by device to VM
    @Serialized private MethodInvocation synchronizedInvocation; // pending main thread invocation

    public RPCAdapter(final DeviceBusController controller, final SerialDevice serialDevice) {
        this(controller, serialDevice, DEFAULT_MAX_MESSAGE_SIZE);
    }

    public RPCAdapter(final DeviceBusController controller, final SerialDevice serialDevice, final int maxMessageSize) {
        this.controller = controller;
        this.serialDevice = serialDevice;
        this.transmitBuffer = ByteBuffer.allocate(maxMessageSize);
        this.gson = DeviceMethodParameterTypeAdapters.beginBuildGson()
                .registerTypeAdapter(MethodInvocation.class, new MethodInvocationJsonDeserializer())
                .registerTypeAdapter(Message.class, new MessageJsonDeserializer())
                .registerTypeAdapter(DeviceInterface.class, new DeviceJsonSerializer())
                .registerTypeAdapter(DeviceMethod.class, new DeviceMethodJsonSerializer())
                .create();
    }

    public void reset() {
        transmitBuffer.clear();
        receiveBuffer = null;
        synchronizedInvocation = null;
    }

    public void tick() {
        if (synchronizedInvocation != null) {
            final MethodInvocation methodInvocation = synchronizedInvocation;
            synchronizedInvocation = null;
            processMethodInvocation(methodInvocation, true);
        }
    }

    public void step(final int cycles) {
        readFromDevice();
        writeToDevice();
    }

    private void readFromDevice() {
        // Only ever allow one pending message to avoid giving the VM the
        // power of uncontrollably inflating memory usage. Basically any
        // method of limiting the write queue size would work, but this is
        // the most simple and easy to maintain one I could think of.
        int value;
        while (receiveBuffer == null && synchronizedInvocation == null && (value = serialDevice.read()) >= 0) {
            if (value == 0) {
                if (transmitBuffer.limit() > 0) {
                    transmitBuffer.flip();
                    if (transmitBuffer.hasRemaining()) {
                        final byte[] message = new byte[transmitBuffer.remaining()];
                        transmitBuffer.get(message);
                        processMessage(message);
                    }
                } else {
                    writeError(ERROR_MESSAGE_TOO_LARGE);
                }
                transmitBuffer.clear();
            } else if (transmitBuffer.hasRemaining()) {
                transmitBuffer.put((byte) value);
            } else {
                transmitBuffer.clear();
                transmitBuffer.limit(0); // marks message too large
            }
        }
    }

    private void writeToDevice() {
        if (receiveBuffer == null) {
            return;
        }

        while (receiveBuffer.hasRemaining() && serialDevice.canPutByte()) {
            serialDevice.putByte(receiveBuffer.get());
        }

        serialDevice.flush();

        if (!receiveBuffer.hasRemaining()) {
            receiveBuffer = null;
        }
    }

    private void processMessage(final byte[] messageData) {
        if (new String(messageData).trim().isEmpty()) {
            return;
        }

        final InputStreamReader stream = new InputStreamReader(new ByteArrayInputStream(messageData));
        try {
            final Message message = gson.fromJson(stream, Message.class);
            switch (message.type) {
                case Message.MESSAGE_TYPE_STATUS: {
                    writeStatus();
                    break;
                }
                case Message.MESSAGE_TYPE_INVOKE_METHOD: {
                    assert message.data != null : "MethodInvocation deserializer produced null data.";
                    processMethodInvocation((MethodInvocation) message.data, false);
                    break;
                }
                default: {
                    writeError(ERROR_UNKNOWN_MESSAGE_TYPE);
                    break;
                }
            }
        } catch (final Throwable e) {
            writeError(e.getMessage());
        }
    }

    private void processMethodInvocation(final MethodInvocation methodInvocation, final boolean isMainThread) {
        final Optional<Device> device = controller.getDevice(methodInvocation.deviceId);
        if (!device.isPresent()) {
            writeError(ERROR_UNKNOWN_DEVICE);
            return;
        }

        // Yes, we could hashmap this lookup, but the expectation is that we'll generally
        // have relatively few methods per object where the overhead of hashing would not
        // be worth it. So we just do a linear search, which also gives us maximal
        // flexibility for free (devices may dynamically change their methods).
        String error = ERROR_UNKNOWN_METHOD;
        outer:
        for (final DeviceMethod method : device.get().getMethods()) {
            if (!Objects.equals(method.getName(), methodInvocation.methodName)) {
                continue;
            }

            final DeviceMethodParameter[] parametersSpec = method.getParameters();
            if (methodInvocation.parameters.size() != parametersSpec.length) {
                error = ERROR_INVALID_PARAMETER_SIGNATURE;
                continue; // There may be an overload with matching parameter count.
            }

            final Object[] parameters = new Object[parametersSpec.length];
            for (int i = 0; i < parametersSpec.length; i++) {
                final DeviceMethodParameter parameterInfo = parametersSpec[i];
                try {
                    parameters[i] = gson.fromJson(methodInvocation.parameters.get(i), parameterInfo.getType());
                } catch (final Throwable e) {
                    error = ERROR_INVALID_PARAMETER_SIGNATURE;
                    continue outer; // There may be an overload with matching parameter types.
                }
            }

            if (method.isSynchronized() && !isMainThread) {
                synchronizedInvocation = methodInvocation;
                return;
            }

            try {
                final Object result = method.invoke(parameters);
                writeMessage(Message.MESSAGE_TYPE_RESULT, result);
            } catch (final Throwable e) {
                writeError(e.getMessage());
            }

            return;
        }

        writeError(error);
    }

    private void writeStatus() {
        writeMessage(Message.MESSAGE_TYPE_STATUS, controller.getDevices().toArray(new DeviceInterface[0]));
    }

    private void writeError(final String message) {
        writeMessage(Message.MESSAGE_TYPE_ERROR, message);
    }

    private void writeMessage(final String type, @Nullable final Object data) {
        if (receiveBuffer != null) throw new IllegalStateException();
        final String json = gson.toJson(new Message(type, data));
        final byte[] bytes = json.getBytes();
        receiveBuffer = ByteBuffer.allocate(bytes.length + MESSAGE_DELIMITER.length * 2);

        // In case we went through a reset and the VM was in the middle of reading
        // a message we inject a delimiter up front to cause the truncated message
        // to be discarded.
        receiveBuffer.put(MESSAGE_DELIMITER);

        receiveBuffer.put(bytes);

        // We follow up each message with a delimiter, too, so the VM knows when the
        // message has been completed. This will lead to two delimiters between most
        // messages. The VM is expected to ignore such "empty" messages.
        receiveBuffer.put(MESSAGE_DELIMITER);

        receiveBuffer.flip();
    }

    public static final class Message {
        // Device -> VM
        public static final String MESSAGE_TYPE_STATUS = "status";
        public static final String MESSAGE_TYPE_RESULT = "result";
        public static final String MESSAGE_TYPE_ERROR = "error";

        // VM -> Device
        public static final String MESSAGE_TYPE_INVOKE_METHOD = "invoke";

        public final String type;
        @Nullable public final Object data;

        public Message(final String type, @Nullable final Object data) {
            this.type = type;
            this.data = data;
        }
    }

    @Serialized
    public static final class MethodInvocation {
        public UUID deviceId;
        public String methodName;
        public JsonArray parameters;

        public MethodInvocation(final UUID deviceId, final String methodName, final JsonArray parameters) {
            this.deviceId = deviceId;
            this.methodName = methodName;
            this.parameters = parameters;
        }
    }
}
