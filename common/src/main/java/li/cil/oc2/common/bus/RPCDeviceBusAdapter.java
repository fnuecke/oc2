/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import com.google.gson.*;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.rpc.*;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceList;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceWithIdentifier;
import li.cil.oc2.common.bus.device.rpc.RPCTypeAdapters;
import li.cil.oc2.common.serialization.gson.*;
import li.cil.oc2.common.util.ThrottledLogger;
import li.cil.sedna.api.device.Steppable;
import li.cil.sedna.api.device.serial.SerialDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;

public final class RPCDeviceBusAdapter implements Steppable {
    private static final Logger LOGGER = LogManager.getLogger(RPCDeviceBusAdapter.class);
    private static final ThrottledLogger THROTTLED_LOGGER = new ThrottledLogger(LOGGER, Duration.ofMinutes(1));

    private static final int DEFAULT_MAX_MESSAGE_SIZE = 4 * Constants.KILOBYTE;

    public static final String ERROR_MESSAGE_TOO_LARGE = "message too large";
    public static final String ERROR_UNKNOWN_MESSAGE_TYPE = "unknown message type";
    public static final String ERROR_UNKNOWN_DEVICE = "unknown device";
    public static final String ERROR_UNKNOWN_METHOD = "unknown method";
    public static final String ERROR_INVALID_PARAMETER_SIGNATURE = "invalid parameter signature";
    public static final String ERROR_PAYLOAD_TOO_LARGE = "payload is larger than the channel allows";
    public static final String ERROR_PAYLOAD_CORRUPT = "payload failed its checksum";
    public static final String ERROR_PAYLOAD_MISMATCH = "payload does not match its description";
    public static final String ERROR_MALFORMED_MESSAGE = "malformed message";
    public static final String ERROR_INTERNAL = "internal error";
    public static final String ERROR_PAYLOAD_NEEDS_UNSYNCHRONIZED =
        "binary parameters (byte[]) require an rpc method to not be synchronized (synchronize = false)";

    // --------------------------------------------------------------------- //

    private final Gson gson;

    @Serialized
    private final RPCDeviceRegistry registry = new RPCDeviceRegistry();
    @Serialized
    private final RPCMessageChannel messages;
    @Serialized
    private final RPCPayloadChannel payloads;
    @Serialized
    private final RPCEventChannel events;
    @Serialized
    private volatile MethodInvocation synchronizedInvocation; // pending main thread invocation
    @Serialized
    private int currentRequestId; // serialized for sync calls

    private final RPCBlobJsonSerializer blobs = new RPCBlobJsonSerializer();
    private final Semaphore pauseLock = new Semaphore(1); // for tryAcquire in step()
    private volatile boolean isPaused; // server thread -> worker thread

    // --------------------------------------------------------------------- //

    public RPCDeviceBusAdapter(final SerialDevice serialDevice, final SerialDevice blobDevice, final SerialDevice eventDevice) {
        this(serialDevice, blobDevice, eventDevice, DEFAULT_MAX_MESSAGE_SIZE);
    }

    public RPCDeviceBusAdapter(final SerialDevice serialDevice, final SerialDevice blobDevice, final SerialDevice eventDevice, final int maxMessageSize) {
        this.messages = new RPCMessageChannel(serialDevice, maxMessageSize);
        this.payloads = new RPCPayloadChannel(blobDevice);
        this.events = new RPCEventChannel(eventDevice);
        this.gson = RPCTypeAdapters.beginBuildGson()
            .registerTypeAdapter(byte[].class, blobs)
            .registerTypeAdapter(MethodInvocation.class, new MethodInvocationJsonDeserializer())
            .registerTypeAdapter(Message.class, new MessageJsonDeserializer())
            .registerTypeAdapter(RPCDeviceWithIdentifier.class, new RPCDeviceWithIdentifierJsonSerializer())
            .registerTypeHierarchyAdapter(RPCMethod.class, new RPCMethodJsonSerializer())
            .registerTypeAdapter(EmptyMethodGroup.class, new EmptyRPCMethodGroupSerializer())
            .registerTypeAdapter(Side.class, new SideJsonDeserializer())
            .create();
    }

    // --------------------------------------------------------------------- //

    public void mountDevices() {
        registry.mountAll();
    }

    public void unmountDevices() {
        registry.unmountAll();
    }

    public void disposeDevices() {
        registry.disposeAll();
    }

    public void reset() {
        messages.reset();
        payloads.reset();
        events.reset();
        blobs.clearPending();
        synchronizedInvocation = null;
    }

    public void pause() {
        if (isPaused) {
            return;
        }

        pauseLock.acquireUninterruptibly();
        isPaused = true;
        pauseLock.release();
    }

    public void resume(final DeviceBusController controller, final boolean didDevicesChange) {
        try {
            if (didDevicesChange) {
                registry.rebuild(controller);
                sendEvent(Message.MESSAGE_TYPE_DEVICES_CHANGED, null);
            }
        } finally {
            isPaused = false;
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
            // reset this when we're done. Otherwise, we may get a race-condition when
            // writing back data.
            synchronizedInvocation = null;
        }
    }

    public void step(final int cycles) {
        if (isPaused || !pauseLock.tryAcquire()) {
            return;
        }

        try {
            payloads.receive();
            readFromDevice();
            writeToDevice();
            announceDroppedEvents();
            events.flush();
        } finally {
            pauseLock.release();
        }
    }

    public boolean sendEvent(final String type, @Nullable final Object data) {
        return events.sendEvent(RPCMessageChannel.frame(
            encode(new Message(type, 0, registry.generation(), data, null))));
    }

    // --------------------------------------------------------------------- //

    private void readFromDevice() {
        // Only ever allow one pending message to avoid giving the VM the
        // power of uncontrollably inflating memory usage. Basically any
        // method of limiting the write queue size would work, but this is
        // the most simple and easy to maintain one I could think of.
        while (!messages.isSending() && !payloads.isSending() && synchronizedInvocation == null) {
            currentRequestId = 0; // until a message says otherwise, a reply cannot name one
            if (!messages.readFrame(this::acceptMessage, () -> writeError(ERROR_MESSAGE_TOO_LARGE))) {
                break;
            }
        }
    }

    private void writeToDevice() {
        payloads.flush();
        messages.flush();
    }

    private void acceptMessage(final byte[] messageData) {
        final BlobReference reference;
        try {
            reference = peekBlobReference(messageData);
        } catch (final Throwable e) {
            payloads.discard();
            writeError(ERROR_MALFORMED_MESSAGE);
            return;
        }

        if (reference == null) {
            payloads.discard(); // Just in case, so the next message doesn't break.
            processMessage(messageData);
            return;
        }

        try {
            blobs.setReceived(payloads.take(reference.length(), reference.checksum()));
        } catch (final RPCPayloadChannel.PayloadException e) {
            writeError(e.error);
            return;
        }

        try {
            processMessage(messageData);
        } finally {
            blobs.setReceived(null);
        }
    }

    @Nullable
    private BlobReference peekBlobReference(final byte[] messageData) {
        final JsonElement parsed = JsonParser.parseString(new String(messageData, StandardCharsets.UTF_8));
        if (!parsed.isJsonObject()) {
            return null;
        }

        final JsonObject message = parsed.getAsJsonObject();
        final JsonElement blob = message.get("blob");
        if (blob == null || !blob.isJsonObject()) {
            return null;
        }

        final JsonElement type = message.get("type");
        if (type == null || !Objects.equals(type.getAsString(), Message.MESSAGE_TYPE_INVOKE_METHOD)) {
            throw new JsonParseException("only an invocation may carry a binary payload");
        }

        final int markers = countBlobMarkers(message);
        if (markers != 1) {
            throw new JsonParseException("a call carrying a payload must refer to it exactly once, saw " + markers);
        }

        final JsonObject reference = blob.getAsJsonObject();
        return new BlobReference(reference.get("length").getAsInt(), reference.get("checksum").getAsInt());
    }

    private static int countBlobMarkers(final JsonElement element) {
        if (element.isJsonObject()) {
            final JsonObject object = element.getAsJsonObject();
            if (object.has(RPCBlobJsonSerializer.BLOB_REFERENCE_KEY)) {
                return 1;
            }
            int count = 0;
            for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
                count += countBlobMarkers(entry.getValue());
            }
            return count;
        }
        if (element.isJsonArray()) {
            int count = 0;
            for (final JsonElement child : element.getAsJsonArray()) {
                count += countBlobMarkers(child);
            }
            return count;
        }
        return 0;
    }

    private void processMessage(final byte[] messageData) {
        if (new String(messageData, StandardCharsets.UTF_8).trim().isEmpty()) {
            return;
        }

        final InputStreamReader stream = new InputStreamReader(new ByteArrayInputStream(messageData), StandardCharsets.UTF_8);
        try {
            final Message message = gson.fromJson(stream, Message.class);
            currentRequestId = message.id;
            switch (message.type) {
                case Message.MESSAGE_TYPE_LIST -> writeDeviceList();
                case Message.MESSAGE_TYPE_METHODS -> {
                    if (message.data instanceof UUID uuid) {
                        writeDeviceMethods(uuid);
                    } else {
                        writeError("missing device id");
                    }
                }
                case Message.MESSAGE_TYPE_INVOKE_METHOD -> {
                    if (message.data instanceof MethodInvocation invocation) {
                        processMethodInvocation(invocation, false);
                    } else {
                        writeError("missing invocation data");
                    }
                }
                default -> writeError(ERROR_UNKNOWN_MESSAGE_TYPE);
            }
        } catch (final JsonParseException e) {
            writeError(ERROR_MALFORMED_MESSAGE);
        } catch (final Throwable e) {
            LOGGER.error("Failed processing RPC message.", e);
            writeError(ERROR_INTERNAL);
        }
    }

    private void processMethodInvocation(final MethodInvocation methodInvocation, final boolean isMainThread) {
        final RPCDevice device = registry.byId(methodInvocation.deviceId);
        if (device == null) {
            writeError(ERROR_UNKNOWN_DEVICE);
            return;
        }

        final RPCInvocation invocation = new RPCInvocationImpl(methodInvocation.parameters, gson);

        // Yes, we could hashmap this lookup, but the expectation is that we'll generally
        // have relatively few methods per object, so the overhead of hashing would not
        // be worth it. Instead, we just do a quick linear search, which also gives us
        // a lot of flexibility for free (devices may dynamically change their methods).
        String error = ERROR_UNKNOWN_METHOD;
        for (final RPCMethodGroup methodGroup : device.getMethodGroups()) {
            if (!Objects.equals(methodGroup.getName(), methodInvocation.methodName)) {
                continue;
            }

            final Optional<RPCMethod> overload = methodGroup.findOverload(invocation);
            if (overload.isPresent()) {
                invokeMethod(methodInvocation, isMainThread, overload.get(), invocation);
                return;
            }

            error = ERROR_INVALID_PARAMETER_SIGNATURE;

            // Keep going, there may be an overload with matching parameter types in another
            // method group with the same name.
        }

        writeError(error);
    }

    private void invokeMethod(final MethodInvocation methodInvocation, final boolean isMainThread, final RPCMethod method, final RPCInvocation invocation) {
        if (method.isSynchronized() && blobs.getReceived() != null) {
            // Blob won't survive until synced callback is run and we don't really need it
            // right now, so we just fail. If this turns out to be needed, we'll have to
            // store the blob next to the sync call. But I don't have the energy to fully
            // think through the implications of that right now, so, yeah.
            writeError(ERROR_PAYLOAD_NEEDS_UNSYNCHRONIZED);
            return;
        }

        if (method.isSynchronized() && !isMainThread) {
            synchronizedInvocation = methodInvocation;
            return;
        }

        try {
            final Object result = method.invoke(invocation);
            writeMessage(Message.MESSAGE_TYPE_RESULT, result);
        } catch (final Throwable e) {
            writeInvocationError(e);
        }
    }

    private void writeInvocationError(final Throwable e) {
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            final String message = e.getMessage();
            writeError(message != null ? message : e.getClass().getSimpleName());
            return;
        }

        THROTTLED_LOGGER.error("Device method invocation failed.", e);

        writeError(ERROR_INTERNAL);
    }

    private void writeDeviceList() {
        writeMessage(Message.MESSAGE_TYPE_LIST, registry.devices());
    }

    private void writeDeviceMethods(final UUID deviceId) {
        final RPCDeviceList device = registry.byId(deviceId);
        if (device != null) {
            writeMessage(Message.MESSAGE_TYPE_METHODS, flattenMethodGroups(device.getMethodGroups()));
        } else {
            writeError("unknown device");
        }
    }

    private List<Object> flattenMethodGroups(final List<? extends RPCMethodGroup> methodGroups) {
        final List<Object> result = new ArrayList<>();
        for (final RPCMethodGroup methodGroup : methodGroups) {
            final Set<RPCMethod> overloads = methodGroup.getOverloads();
            if (overloads.isEmpty()) {
                result.add(new EmptyMethodGroup(methodGroup.getName()));
            } else {
                result.addAll(overloads);
            }
        }
        return result;
    }

    private void writeError(final String message) {
        writeMessage(Message.MESSAGE_TYPE_ERROR, message);
    }

    private void writeMessage(final String type, @Nullable final Object data) {
        blobs.clearPending();
        final JsonElement dataElement = gson.toJsonTree(data);

        BlobReference blob = null;
        final byte[] pending = blobs.getPending();
        if (pending != null) {
            if (!Objects.equals(type, Message.MESSAGE_TYPE_RESULT)) {
                throw new IllegalStateException("only a result may carry a binary payload");
            }
            if (pending.length > Constants.RPC_MAX_PAYLOAD_SIZE) {
                writeError(ERROR_PAYLOAD_TOO_LARGE);
                return;
            }

            blob = new BlobReference(pending.length, RPCPayloadChannel.checksum(pending));
            payloads.send(pending);
        }

        messages.send(RPCMessageChannel.frame(encode(
            new Message(type, currentRequestId, registry.generation(), dataElement, blob))));
    }

    private void announceDroppedEvents() {
        final int dropped = events.takeDropped();
        if (dropped > 0) {
            events.sendNotice(RPCMessageChannel.frame(encode(new Message(
                Message.MESSAGE_TYPE_EVENTS_DROPPED, 0, registry.generation(), dropped, null))));
        }
    }

    private byte[] encode(final Message message) {
        return gson.toJson(message).getBytes(StandardCharsets.UTF_8);
    }

    // --------------------------------------------------------------------- //

    public record EmptyMethodGroup(String name) {
    }

    public record BlobReference(int length, int checksum) {
    }

    public record Message(String type, int id, int gen,
                          @Nullable Object data, @Nullable BlobReference blob) {
        // Device -> VM
        public static final String MESSAGE_TYPE_LIST = "list";
        public static final String MESSAGE_TYPE_METHODS = "methods";
        public static final String MESSAGE_TYPE_RESULT = "result";
        public static final String MESSAGE_TYPE_ERROR = "error";
        public static final String MESSAGE_TYPE_DEVICES_CHANGED = "devicesChanged"; // event
        public static final String MESSAGE_TYPE_EVENTS_DROPPED = "eventsDropped"; // event

        // VM -> Device
        public static final String MESSAGE_TYPE_INVOKE_METHOD = "invoke";
    }

    @Serialized
    public static final class MethodInvocation {
        public UUID deviceId;
        public String methodName;
        public JsonArray parameters;

        @SuppressWarnings("unused") // For deserialization.
        public MethodInvocation() {
        }

        public MethodInvocation(final UUID deviceId, final String methodName, final JsonArray parameters) {
            this.deviceId = deviceId;
            this.methodName = methodName;
            this.parameters = parameters;
        }
    }

    // --------------------------------------------------------------------- //

    private record RPCInvocationImpl(JsonArray parameters, Gson gson) implements RPCInvocation {
        @Override
        public JsonArray getParameters() {
            return parameters;
        }

        @Override
        public Gson getGson() {
            return gson;
        }

        @Override
        public Optional<Object[]> tryDeserializeParameters(final RPCParameter... parameterTypes) {
            if (parameterTypes.length != parameters.size()) {
                return Optional.empty();
            }

            final Object[] result = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                final RPCParameter parameterInfo = parameterTypes[i];
                try {
                    result[i] = gson.fromJson(parameters.get(i), parameterInfo.getType());
                } catch (final Throwable e) {
                    return Optional.empty();
                }
            }
            return Optional.of(result);
        }
    }
}
