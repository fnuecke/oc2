package li.cil.oc2.common.bus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceList;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.serialization.serializers.*;
import li.cil.sedna.api.device.Steppable;
import li.cil.sedna.api.device.serial.SerialDevice;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class RPCDeviceBusAdapter implements Steppable {
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 4 * Constants.KILOBYTE;
    private static final byte[] MESSAGE_DELIMITER = "\0".getBytes();

    public static final String ERROR_MESSAGE_TOO_LARGE = "message too large";
    public static final String ERROR_UNKNOWN_MESSAGE_TYPE = "unknown message type";
    public static final String ERROR_UNKNOWN_DEVICE = "unknown device";
    public static final String ERROR_UNKNOWN_METHOD = "unknown method";
    public static final String ERROR_INVALID_PARAMETER_SIGNATURE = "invalid parameter signature";

    ///////////////////////////////////////////////////////////////////

    private final SerialDevice serialDevice;
    private final Gson gson;

    private final ArrayList<RPCDeviceWithIdentifier> devices = new ArrayList<>();
    private final HashMap<UUID, RPCDeviceList> devicesById = new HashMap<>();
    private final Set<RPCDevice> unmountedDevices = new HashSet<>();
    private final Set<RPCDevice> mountedDevices = new HashSet<>();
    private final Lock pauseLock = new ReentrantLock();
    private boolean isPaused;

    ///////////////////////////////////////////////////////////////////

    @Serialized private final ByteBuffer transmitBuffer; // for data written to device by VM
    @Serialized private ByteBuffer receiveBuffer; // for data written by device to VM
    @Serialized private MethodInvocation synchronizedInvocation; // pending main thread invocation

    ///////////////////////////////////////////////////////////////////

    public RPCDeviceBusAdapter(final SerialDevice serialDevice) {
        this(serialDevice, DEFAULT_MAX_MESSAGE_SIZE);
    }

    public RPCDeviceBusAdapter(final SerialDevice serialDevice, final int maxMessageSize) {
        this.serialDevice = serialDevice;
        this.transmitBuffer = ByteBuffer.allocate(maxMessageSize);
        this.gson = RPCMethodParameterTypeAdapters.beginBuildGson()
            .registerTypeAdapter(byte[].class, new UnsignedByteArrayJsonSerializer())
            .registerTypeAdapter(MethodInvocation.class, new MethodInvocationJsonDeserializer())
            .registerTypeAdapter(Message.class, new MessageJsonDeserializer())
            .registerTypeAdapter(RPCDeviceWithIdentifier.class, new RPCDeviceWithIdentifierJsonSerializer())
            .registerTypeHierarchyAdapter(RPCMethod.class, new RPCMethodJsonSerializer())
            .create();
    }

    ///////////////////////////////////////////////////////////////////

    public void mount() {
        for (final RPCDevice device : unmountedDevices) {
            device.mount();
        }
        mountedDevices.addAll(unmountedDevices);
        unmountedDevices.clear();
    }

    public void unmount() {
        for (final RPCDevice device : mountedDevices) {
            device.unmount();
        }
        unmountedDevices.addAll(mountedDevices);
        mountedDevices.clear();
    }

    public void suspend() {
        for (final RPCDeviceWithIdentifier info : devices) {
            info.device.suspend();
        }
    }

    public void reset() {
        transmitBuffer.clear();
        receiveBuffer = null;
        synchronizedInvocation = null;
    }

    public void pause() {
        if (isPaused) {
            return;
        }

        pauseLock.lock();
        isPaused = true;
        pauseLock.unlock();
    }

    public void resume(final DeviceBusController controller, final boolean didDevicesChange) {
        isPaused = false;

        if (!didDevicesChange) {
            return;
        }

        devices.clear();
        devicesById.clear();
        unmountedDevices.clear();

        // How device grouping works:
        // Each device can have multiple UUIDs due to being attached to multiple bus elements.
        // There is no guarantee that for each device D1 present on bus elements E1 and E2,
        // where device D2 is present on E1 it will also be present on E2. This is completely
        // up to the device providers.
        // Therefore we must group all devices by their identifiers to then remove duplicate
        // groups. This is fragile because it will depend on the order the devices appear in
        // the list. However, since we add devices to bus elements in the order of their
        // providers, then add devices to the controller in the order of their elements, this
        // will work. And even if it does not, it only leads to duplicate devices popping up
        // in the VM, which, while annoying, is not breaking anything.
        // In a final step, when we know which devices are duplicates and what identifiers
        // they have, we pick a single identifier in a deterministic way, given the list of
        // identifiers is the same.

        final HashMap<UUID, ArrayList<RPCDevice>> devicesByIdentifier = new HashMap<>();
        for (final Device device : controller.getDevices()) {
            if (device instanceof final RPCDevice rpcDevice) {
                final Set<UUID> identifiers = controller.getDeviceIdentifiers(device);
                for (final UUID identifier : identifiers) {
                    devicesByIdentifier
                        .computeIfAbsent(identifier, unused -> new ArrayList<>())
                        .add(rpcDevice);
                }
            }
        }

        final HashMap<RPCDeviceList, ArrayList<UUID>> identifiersByDevice = new HashMap<>();
        devicesByIdentifier.forEach((identifier, devices) -> {
            final RPCDeviceList device = new RPCDeviceList(devices);

            // If there are no methods we have either no devices at all, or all synthetic
            // devices, i.e. devices that only contribute type names, but have no methods
            // to call. We do not expose these to avoid cluttering the device list.
            if (device.getMethods().isEmpty()) {
                return;
            }

            identifiersByDevice
                .computeIfAbsent(device, unused -> new ArrayList<>())
                .add(identifier);
        });

        final Set<RPCDevice> newDevices = new HashSet<>();
        identifiersByDevice.forEach((device, identifiers) -> {
            final UUID identifier = selectIdentifierDeterministically(identifiers);
            devices.add(new RPCDeviceWithIdentifier(identifier, device));
            devicesById.put(identifier, device);
            newDevices.add(device);
        });

        // Add new devices to list of unmounted devices. List was cleared, so removed devices previously in
        // list of unmounted devices are already gone.
        for (final RPCDevice newDevice : newDevices) {
            if (!mountedDevices.contains(newDevice)) {
                unmountedDevices.add(newDevice);
            }
        }

        // Remove removed devices from list of mounted devices.
        final Iterator<RPCDevice> mountedDeviceIterator = mountedDevices.iterator();
        while (mountedDeviceIterator.hasNext()) {
            final RPCDevice device = mountedDeviceIterator.next();
            if (!newDevices.contains(device)) {
                device.unmount();
                mountedDeviceIterator.remove();
            }
        }
    }

    public void tick() {
        if (isPaused) {
            return;
        }

        if (synchronizedInvocation != null) {
            final MethodInvocation methodInvocation = synchronizedInvocation;
            processMethodInvocation(methodInvocation, true);

            // This is also used to prevent thread from processing messages, so only
            // reset this when we're done. Otherwise we may get a race-condition when
            // writing back data.
            synchronizedInvocation = null;
        }
    }

    public void step(final int cycles) {
        if (isPaused || !pauseLock.tryLock()) {
            return;
        }

        try {
            readFromDevice();
            writeToDevice();
        } finally {
            pauseLock.unlock();
        }
    }

    ///////////////////////////////////////////////////////////////////

    private UUID selectIdentifierDeterministically(final ArrayList<UUID> identifiers) {
        UUID lowestIdentifier = identifiers.get(0);
        for (int i = 1; i < identifiers.size(); i++) {
            final UUID identifier = identifiers.get(i);
            if (identifier.compareTo(lowestIdentifier) < 0) {
                lowestIdentifier = identifier;
            }
        }
        return lowestIdentifier;
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
                case Message.MESSAGE_TYPE_LIST -> writeDeviceList();
                case Message.MESSAGE_TYPE_METHODS -> {
                    if (message.data != null) {
                        writeDeviceMethods((UUID) message.data);
                    } else {
                        writeError("missing device id");
                    }
                }
                case Message.MESSAGE_TYPE_INVOKE_METHOD -> {
                    if (message.data != null) {
                        processMethodInvocation((MethodInvocation) message.data, false);
                    } else {
                        writeError("missing invocation data");
                    }
                }
                default -> writeError(ERROR_UNKNOWN_MESSAGE_TYPE);
            }
        } catch (final Throwable e) {
            writeError(e.getMessage());
        }
    }

    private void processMethodInvocation(final MethodInvocation methodInvocation, final boolean isMainThread) {
        final RPCDevice device = devicesById.get(methodInvocation.deviceId);
        if (device == null) {
            writeError(ERROR_UNKNOWN_DEVICE);
            return;
        }

        // Yes, we could hashmap this lookup, but the expectation is that we'll generally
        // have relatively few methods per object where the overhead of hashing would not
        // be worth it. So we just do a linear search, which also gives us maximal
        // flexibility for free (devices may dynamically change their methods).
        final List<RPCMethod> fallbacks = new ArrayList<>();
        String error = ERROR_UNKNOWN_METHOD;
        for (final RPCMethod method : device.getMethods()) {
            if (!Objects.equals(method.getName(), methodInvocation.methodName)) {
                continue;
            }

            final RPCParameter[] parametersSpec = method.getParameters();

            // Special case: if a method takes as exactly one parameter a JsonArray, we pass
            // on the parameters as-is, without automatically trying to deserialize them.
            if (parametersSpec.length == 1 && parametersSpec[0].getType() == JsonArray.class) {
                invokeMethod(methodInvocation, isMainThread, method, new Object[]{methodInvocation.parameters});
                return;
            }

            if (methodInvocation.parameters.size() != parametersSpec.length) {
                if (canTrailingParametersBeImplicitlyNull(methodInvocation.parameters, parametersSpec)) {
                    fallbacks.add(method);
                }

                error = ERROR_INVALID_PARAMETER_SIGNATURE;
                continue; // There may be an overload with matching parameter count.
            }

            final Object[] parameters = getParameters(methodInvocation.parameters, parametersSpec);
            if (parameters == null) {
                error = ERROR_INVALID_PARAMETER_SIGNATURE;
                continue; // There may be an overload with matching parameter types.
            }

            invokeMethod(methodInvocation, isMainThread, method, parameters);

            return;
        }

        if (fallbacks.size() == 1) {
            final RPCMethod method = fallbacks.get(0);
            final Object[] parameters = getParameters(methodInvocation.parameters, method.getParameters());
            if (parameters != null) {
                invokeMethod(methodInvocation, isMainThread, method, parameters);
                return;
            }
        }

        writeError(error);
    }

    private void invokeMethod(final MethodInvocation methodInvocation, final boolean isMainThread, final RPCMethod method, final Object[] parameters) {
        if (method.isSynchronized() && !isMainThread) {
            synchronizedInvocation = methodInvocation;
            return;
        }

        try {
            final Object result = method.invoke(parameters);
            writeMessage(Message.MESSAGE_TYPE_RESULT, result);
        } catch (final Throwable e) {
            writeError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    @Nullable
    private Object[] getParameters(final JsonArray parameters, final RPCParameter[] parametersSpec) {
        final Object[] result = new Object[parametersSpec.length];
        for (int i = 0; i < parametersSpec.length; i++) {
            final RPCParameter parameterInfo = parametersSpec[i];
            if (parameters.size() > i) {
                try {
                    result[i] = gson.fromJson(parameters.get(i), parameterInfo.getType());
                } catch (final Throwable e) {
                    return null;
                }
            } else {
                result[i] = null;
            }
        }
        return result;
    }

    private boolean canTrailingParametersBeImplicitlyNull(final JsonArray parameters, final RPCParameter[] parametersSpec) {
        if (parameters.size() > parametersSpec.length) {
            return false;
        }

        for (int i = parameters.size(); i < parametersSpec.length; i++) {
            final Class<?> type = parametersSpec[i].getType();
            if (type.isPrimitive()) {
                return false;
            }
        }

        return true;
    }

    private void writeDeviceList() {
        writeMessage(Message.MESSAGE_TYPE_LIST, devices);
    }

    private void writeDeviceMethods(final UUID deviceId) {
        final RPCDeviceList device = devicesById.get(deviceId);
        if (device != null) {
            writeMessage(Message.MESSAGE_TYPE_METHODS, device.getMethods());
        } else {
            writeError("unknown device");
        }
    }

    private void writeError(final String message) {
        writeMessage(Message.MESSAGE_TYPE_ERROR, message);
    }

    private void writeMessage(final String type, @Nullable final Object data) {
        if (receiveBuffer != null) throw new IllegalStateException();
        final String json = gson.toJson(new Message(type, data));
        final byte[] bytes = json.getBytes();
        final ByteBuffer receiveBuffer = ByteBuffer.allocate(bytes.length + MESSAGE_DELIMITER.length * 2);

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
        this.receiveBuffer = receiveBuffer;
    }

    ///////////////////////////////////////////////////////////////////

    public static final class RPCDeviceWithIdentifier {
        public final UUID identifier;
        public final RPCDevice device;

        private RPCDeviceWithIdentifier(final UUID identifier, final RPCDevice device) {
            this.identifier = identifier;
            this.device = device;
        }
    }

    public static final class Message {
        // Device -> VM
        public static final String MESSAGE_TYPE_LIST = "list";
        public static final String MESSAGE_TYPE_METHODS = "methods";
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

        public MethodInvocation() { // For deserialization.
        }

        public MethodInvocation(final UUID deviceId, final String methodName, final JsonArray parameters) {
            this.deviceId = deviceId;
            this.methodName = methodName;
            this.parameters = parameters;
        }
    }
}
