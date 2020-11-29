package li.cil.oc2.common.bus;

import com.google.gson.*;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.DeviceMethod;
import li.cil.oc2.api.device.DeviceMethodParameter;
import li.cil.oc2.api.device.IdentifiableDevice;
import li.cil.oc2.common.util.TileEntityUtils;
import li.cil.sedna.api.device.Steppable;
import li.cil.sedna.api.device.serial.SerialDevice;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceBusControllerImpl implements DeviceBusController, Steppable {
    public enum State {
        SCAN_PENDING,
        TOO_COMPLEX,
        READY,
    }

    private static final int MAX_BUS_ELEMENT_COUNT = 128;
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 32 * 1024;
    private static final byte[] MESSAGE_DELIMITER = "\0".getBytes();

    // Device -> VM
    private static final String MESSAGE_TYPE_STATUS = "status";
    private static final String MESSAGE_TYPE_RESULT = "result";
    private static final String MESSAGE_TYPE_ERROR = "error";

    private static final String ERROR_MESSAGE_TOO_LARGE = "message too large";
    private static final String ERROR_UNKNOWN_MESSAGE_TYPE = "unknown message type";
    private static final String ERROR_UNKNOWN_DEVICE = "unknown device";
    private static final String ERROR_UNKNOWN_METHOD = "unknown method";
    private static final String ERROR_INVALID_PARAMETER_SIGNATURE = "invalid parameter signature";

    // VM -> Device
    private static final String MESSAGE_TYPE_INVOKE_METHOD = "invoke";

    private final Set<DeviceBusElement> elements = new HashSet<>();
    private final ConcurrentHashMap<UUID, IdentifiableDevice> devices = new ConcurrentHashMap<>();

    private final SerialDevice serialDevice;
    private final Gson gson;
    private int scanDelay;

    @Serialized private final ByteBuffer transmitBuffer; // for data written to device by VM
    @Serialized private ByteBuffer receiveBuffer; // for data written by device to VM
    @Serialized private MethodInvocation synchronizedInvocation; // pending main thread invocation

    public DeviceBusControllerImpl(final SerialDevice serialDevice) {
        this(serialDevice, DEFAULT_MAX_MESSAGE_SIZE);
    }

    public DeviceBusControllerImpl(final SerialDevice serialDevice, final int maxMessageSize) {
        this.serialDevice = serialDevice;
        this.transmitBuffer = ByteBuffer.allocate(maxMessageSize);
        this.gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(MethodInvocation.class, new MethodInvocationDeserializer())
                .registerTypeAdapter(Message.class, new MessageDeserializer())
                .registerTypeAdapter(Device.class, new DeviceSerializer())
                .registerTypeAdapter(DeviceMethod.class, new DeviceMethodSerializer())
                .create();
    }

    @Override
    public void scheduleBusScan() {
        for (final DeviceBusElement element : elements) {
            assert element.getController().isPresent() && element.getController().get() == this;
            element.setController(null);
        }

        elements.clear();
        devices.clear();

        scanDelay = 0; // scan as soon as possible
    }

    @Override
    public void scanDevices() {
        devices.clear();
        for (final DeviceBusElement element : elements) {
            for (final IdentifiableDevice device : element.getLocalDevices()) {
                final UUID uuid = device.getUniqueId();
                devices.putIfAbsent(uuid, device);
            }
        }
    }

    @Override
    public Collection<IdentifiableDevice> getDevices() {
        return devices.values();
    }

    public State scan(final World world, final BlockPos start) {
        if (scanDelay < 0) {
            return State.READY;
        }

        if (scanDelay-- > 0) {
            return State.SCAN_PENDING;
        }

        assert scanDelay == -1;

        final Stack<ScanEdge> queue = new Stack<>();
        final HashSet<ScanEdge> seenEdges = new HashSet<>(); // to avoid duplicate edge scans
        final HashSet<BlockPos> busPositions = new HashSet<>(); // to track number of seen blocks for limit

        final Direction[] faces = Direction.values();
        for (final Direction face : faces) {
            final ScanEdge edgeIn = new ScanEdge(start, face);
            queue.add(edgeIn);
            seenEdges.add(edgeIn);
        }

        while (!queue.isEmpty()) {
            final ScanEdge edge = queue.pop();
            assert seenEdges.contains(edge);

            final ChunkPos chunkPos = new ChunkPos(edge.position);
            if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
                // If we have an unloaded chunk neighbor we cannot know whether our neighbor in that
                // chunk would cause a scan once it is loaded, so we'll just retry every so often.
                scanDelay = 20;
                elements.clear();
                return State.SCAN_PENDING;
            }

            final TileEntity tileEntity = world.getTileEntity(edge.position);
            if (tileEntity == null) {
                for (final Direction face : faces) {
                    seenEdges.add(new ScanEdge(edge.position, face));
                }

                continue;
            }

            final Optional<DeviceBusElement> capability = TileEntityUtils.getInterfaceForSide(tileEntity, DeviceBusElement.class, edge.face);
            if (capability.isPresent()) {
                if (busPositions.add(edge.position) && busPositions.size() > MAX_BUS_ELEMENT_COUNT) {
                    elements.clear();
                    return State.TOO_COMPLEX;
                }

                final DeviceBusElement element = capability.get();
                elements.add(element);

                for (final Direction face : faces) {
                    final Optional<DeviceBusElement> otherCapability = TileEntityUtils.getInterfaceForSide(tileEntity, DeviceBusElement.class, face);
                    otherCapability.ifPresent(otherElement -> {
                        final boolean isConnectedToIncomingEdge = otherElement == element;
                        if (!isConnectedToIncomingEdge) {
                            return;
                        }

                        final ScanEdge edgeIn = new ScanEdge(edge.position, face);
                        seenEdges.add(edgeIn);

                        final ScanEdge edgeOut = new ScanEdge(edge.position, face.getOpposite());
                        if (seenEdges.add(edgeOut)) {
                            queue.add(edgeOut);
                        }
                    });
                }
            }
        }

        for (final DeviceBusElement element : elements) {
            assert !element.getController().isPresent();
            element.setController(this);
        }

        scanDevices();

        return State.READY;
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
                case MESSAGE_TYPE_STATUS: {
                    writeStatus();
                    break;
                }
                case MESSAGE_TYPE_INVOKE_METHOD: {
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
        final Device device = devices.get(methodInvocation.deviceId);
        if (device == null) {
            writeError(ERROR_UNKNOWN_DEVICE);
            return;
        }

        // Yes, we could hashmap this lookup, but the expectation is that we'll generally
        // have relatively few methods per object where the overhead of hashing would not
        // be worth it. So we just do a linear search, which also gives us maximal
        // flexibility for free (devices may dynamically change their methods).
        String error = ERROR_UNKNOWN_METHOD;
        outer:
        for (final DeviceMethod method : device.getMethods()) {
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
                writeMessage(MESSAGE_TYPE_RESULT, result);
            } catch (final Throwable e) {
                writeError(e.getMessage());
            }

            return;
        }

        writeError(error);
    }

    private void writeStatus() {
        writeMessage(MESSAGE_TYPE_STATUS, devices.values().toArray(new Device[0]));
    }

    private void writeError(final String message) {
        writeMessage(MESSAGE_TYPE_ERROR, message);
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

    private static final class ScanEdge {
        public final BlockPos position;
        public final Direction face;

        public ScanEdge(final BlockPos position, final Direction face) {
            this.position = position;
            this.face = face;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ScanEdge scanEdge = (ScanEdge) o;
            return position.equals(scanEdge.position) &&
                   face == scanEdge.face;
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, face);
        }
    }

    private static final class Message {
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

    private static final class MessageDeserializer implements JsonDeserializer<Message> {
        @Override
        public Message deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            final String messageType = jsonObject.get("type").getAsString();
            final Object messageData;
            switch (messageType) {
                case MESSAGE_TYPE_STATUS: {
                    messageData = null;
                    break;
                }
                case MESSAGE_TYPE_INVOKE_METHOD: {
                    messageData = context.deserialize(jsonObject.getAsJsonObject("data"), MethodInvocation.class);
                    break;
                }
                default: {
                    throw new JsonParseException(ERROR_UNKNOWN_MESSAGE_TYPE);
                }
            }

            return new Message(messageType, messageData);
        }
    }

    private static final class MethodInvocationDeserializer implements JsonDeserializer<MethodInvocation> {
        @Override
        public MethodInvocation deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            final UUID deviceId = context.deserialize(jsonObject.get("deviceId"), UUID.class);
            final String methodName = jsonObject.get("name").getAsString();
            final JsonArray parameters = jsonObject.getAsJsonArray("parameters");
            return new MethodInvocation(deviceId, methodName, parameters != null ? parameters : new JsonArray());
        }
    }

    private static final class DeviceSerializer implements JsonSerializer<IdentifiableDevice> {
        @Override
        public JsonElement serialize(final IdentifiableDevice src, final Type typeOfSrc, final JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }

            final JsonObject deviceJson = new JsonObject();
            deviceJson.add("deviceId", context.serialize(src.getUniqueId()));
            deviceJson.add("typeNames", context.serialize(src.getTypeNames()));

            final JsonArray methodsJson = new JsonArray();
            deviceJson.add("methods", methodsJson);
            for (final DeviceMethod method : src.getMethods()) {
                methodsJson.add(context.serialize(method, DeviceMethod.class));
            }

            return deviceJson;
        }
    }

    private static final class DeviceMethodSerializer implements JsonSerializer<DeviceMethod> {
        @Override
        public JsonElement serialize(final DeviceMethod method, final Type typeOfMethod, final JsonSerializationContext context) {
            if (method == null) {
                return JsonNull.INSTANCE;
            }

            final JsonObject methodJson = new JsonObject();
            methodJson.addProperty("name", method.getName());
            methodJson.addProperty("returnType", method.getReturnType().getSimpleName());

            method.getDescription().ifPresent(s -> methodJson.addProperty("description", s));
            method.getReturnValueDescription().ifPresent(s -> methodJson.addProperty("returnValueDescription", s));

            final JsonArray parametersJson = new JsonArray();
            methodJson.add("parameters", parametersJson);

            final DeviceMethodParameter[] parameters = method.getParameters();
            for (final DeviceMethodParameter parameter : parameters) {
                final JsonObject parameterJson = new JsonObject();

                parameter.getName().ifPresent(s -> parameterJson.addProperty("name", s));
                parameter.getDescription().ifPresent(s -> parameterJson.addProperty("description", s));

                final Class<?> type = parameter.getType();
                parameterJson.addProperty("type", type.getSimpleName());

                parametersJson.add(parameterJson);
            }

            return methodJson;
        }
    }
}
