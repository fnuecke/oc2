package li.cil.oc2.common.vm;

import com.google.gson.*;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.rpc.*;
import li.cil.oc2.common.bus.RPCDeviceBusAdapter;
import li.cil.sedna.api.device.serial.SerialDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RPCAdapterTests {
    private static final UUID DEVICE_UUID = java.util.UUID.randomUUID();

    private TestSerialDevice serialDevice;
    private DeviceBusController busController;
    private RPCDeviceBusAdapter rpcAdapter;

    @BeforeEach
    public void setupEach() {
        serialDevice = new TestSerialDevice();
        busController = mock(DeviceBusController.class);
        rpcAdapter = new RPCDeviceBusAdapter(serialDevice);
    }

    @Test
    public void resetAndReadDescriptor() {
        final VoidIntMethod method = new VoidIntMethod();
        final TestRPCDevice device = new TestRPCDevice(method);
        setDevice(device, DEVICE_UUID);

        final JsonObject request = new JsonObject();
        request.addProperty("type", "list");
        serialDevice.putAsVM(request.toString());
        rpcAdapter.step(0); // process message

        final String message = serialDevice.readMessageAsVM();
        assertNotNull(message);

        final JsonObject json = JsonParser.parseString(message).getAsJsonObject();

        final JsonArray devicesJson = json.getAsJsonArray("data");
        assertEquals(1, devicesJson.size());

        final JsonObject deviceJson = devicesJson.get(0).getAsJsonObject();

        assertNotNull(deviceJson.get("deviceId"));
        assertNotNull(deviceJson.get("typeNames"));
    }

    @Test
    public void simpleMethod() {
        final VoidIntMethod method = new VoidIntMethod();
        final TestRPCDevice device = new TestRPCDevice(method);
        setDevice(device, DEVICE_UUID);

        invokeMethod(DEVICE_UUID, method.getName(), 0xdeadbeef);

        assertEquals(0xdeadbeef, method.passedValue);
    }

    @Test
    public void returningMethod() {
        final IntLongMethod method = new IntLongMethod();
        final TestRPCDevice device = new TestRPCDevice(method);
        setDevice(device, DEVICE_UUID);

        final JsonElement result = invokeMethod(DEVICE_UUID, method.getName(), 0xdeadbeefcafebabeL);
        assertNotNull(result);
        assertTrue(result.isJsonPrimitive());
        assertEquals(0xcafebabe, result.getAsInt());
    }

    @Test
    public void annotatedObject() {
        final SimpleObject object = new SimpleObject();
        final ObjectDevice device = new ObjectDevice(object);
        setDevice(device, DEVICE_UUID);

        assertEquals(42 + 23, invokeMethod(DEVICE_UUID, "add", 42, 23).getAsInt());
    }

    @Test
    public void customDeviceGroupRejectsInvalidParameters() {
        final RPCDevice device = mock(RPCDevice.class);
        when(device.getTypeNames()).thenReturn(Collections.singletonList("test"));
        when(device.getMethodGroups()).thenReturn(Collections.singletonList(new CustomMethodGroup()));
        setDevice(device, DEVICE_UUID);

        assertEquals("error", invokeMethodRaw(DEVICE_UUID, "test", "invalid", 123).get("type").getAsString());
    }

    @Test
    public void customDeviceGroupAcceptsValidParameters() {
        final RPCDevice device = mock(RPCDevice.class);
        when(device.getTypeNames()).thenReturn(Collections.singletonList("test"));
        when(device.getMethodGroups()).thenReturn(Collections.singletonList(new CustomMethodGroup()));
        setDevice(device, DEVICE_UUID);

        assertEquals(42, invokeMethod(DEVICE_UUID, "test", 123, "valid").getAsInt());
    }

    private void setDevice(final RPCDevice device, final UUID deviceId) {
        when(busController.getDevices()).thenReturn(singleton(device));
        when(busController.getDeviceIdentifiers(device)).thenReturn(singleton(deviceId));

        // trigger device cache rebuild
        rpcAdapter.resume(busController, true);
    }

    private JsonObject invokeMethodRaw(final UUID deviceId, final String name, final Object... parameters) {
        final JsonObject request = new JsonObject();
        request.addProperty("type", "invoke");
        final JsonObject methodInvocation = new JsonObject();
        methodInvocation.addProperty("deviceId", deviceId.toString());
        methodInvocation.addProperty("name", name);
        final JsonArray parametersJson = new JsonArray();
        methodInvocation.add("parameters", parametersJson);
        for (final Object parameter : parameters) {
            parametersJson.add(new Gson().toJsonTree(parameter));
        }
        request.add("data", methodInvocation);
        serialDevice.putAsVM(request.toString());

        rpcAdapter.step(0);

        final String result = serialDevice.readMessageAsVM();
        assertNotNull(result);
        return JsonParser.parseString(result).getAsJsonObject();
    }

    private JsonElement invokeMethod(final UUID deviceId, final String name, final Object... parameters) {
        final JsonObject resultJson = invokeMethodRaw(deviceId, name, parameters);
        assertEquals("result", resultJson.get("type").getAsString());
        return resultJson.get("data");
    }

    private static final class VoidIntMethod extends AbstractTestMethod {
        public int passedValue;

        VoidIntMethod() {
            super(void.class, int.class);
        }

        @Override
        public Object invoke(final Object... parameters) {
            passedValue = (int) parameters[0];
            return 0;
        }
    }

    private static final class IntLongMethod extends AbstractTestMethod {
        public long passedValue;

        IntLongMethod() {
            super(int.class, long.class);
        }

        @Override
        public Object invoke(final Object... parameters) {
            passedValue = (long) parameters[0];
            return (int) passedValue;
        }
    }

    private static final class CustomMethodGroup implements RPCMethodGroup {
        @Override
        public String getName() {
            return "test";
        }

        @Override
        public Optional<RPCMethod> findOverload(final RPCInvocation invocation) {
            final JsonArray parameters = invocation.getParameters();
            if (parameters.size() != 2)
                return Optional.empty();
            if (!parameters.get(0).isJsonPrimitive() || !parameters.get(0).getAsJsonPrimitive().isNumber())
                return Optional.empty();
            if (!parameters.get(1).isJsonPrimitive() || !parameters.get(1).getAsJsonPrimitive().isString())
                return Optional.empty();

            return Optional.of(new AbstractRPCMethod("test", int.class, () -> int.class, () -> String.class) {
                @Override
                protected Object invoke(final Object... parameters) {
                    return 42;
                }
            });
        }
    }

    public static final class SimpleObject {
        @Callback(synchronize = false)
        public int add(@Parameter("a") final int a,
                       @Parameter("b") final int b) {
            return a + b;
        }

        @Callback(synchronize = false)
        public int div(@Parameter("a") final long a,
                       @Parameter("b") final long b) {
            return (int) (a / b);
        }
    }

    private static final class TestSerialDevice implements SerialDevice {
        private final ByteArrayFIFOQueue transmit = new ByteArrayFIFOQueue();
        private final ByteArrayFIFOQueue receive = new ByteArrayFIFOQueue();

        public void putAsVM(final String data) {
            final byte[] bytes = data.getBytes();
            for (int i = 0; i < bytes.length; i++) {
                transmit.enqueue(bytes[i]);
            }
            transmit.enqueue((byte) 0);
        }

        @Nullable
        public String readMessageAsVM() {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (!receive.isEmpty()) {
                final byte value = receive.dequeueByte();

                if (value == 0) {
                    if (bytes.size() == 0) {
                        continue;
                    } else {
                        break;
                    }
                }

                bytes.write(value);
            }

            if (bytes.size() > 0) {
                return bytes.toString();
            } else {
                return null;
            }
        }

        @Override
        public int read() {
            return transmit.isEmpty() ? -1 : transmit.dequeueByte();
        }

        @Override
        public boolean canPutByte() {
            return true;
        }

        @Override
        public void putByte(final byte value) {
            receive.enqueue(value);
        }
    }

    private static final class TestRPCDevice implements RPCDevice {
        private final RPCMethod method;

        public TestRPCDevice(final RPCMethod method) {
            this.method = method;
        }

        @Override
        public List<String> getTypeNames() {
            return singletonList(getClass().getSimpleName());
        }

        @Override
        public List<RPCMethodGroup> getMethodGroups() {
            return singletonList(method);
        }
    }
}
