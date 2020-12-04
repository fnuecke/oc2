package li.cil.oc2.vm;

import com.google.gson.*;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.common.bus.RPCAdapter;
import li.cil.sedna.api.device.serial.SerialDevice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RPCAdapterTests {
    private static final UUID DEVICE_UUID = java.util.UUID.randomUUID();

    private TestSerialDevice serialDevice;
    private DeviceBusController busController;
    private RPCAdapter rpcAdapter;

    @BeforeEach
    public void setupEach() {
        serialDevice = new TestSerialDevice();
        busController = mock(DeviceBusController.class);
        rpcAdapter = new RPCAdapter(busController, serialDevice);
    }

    @Test
    public void resetAndReadDescriptor() {
        final VoidIntMethod method = new VoidIntMethod();
        final TestRPCDeviceInterface device = new TestRPCDeviceInterface(method);
        setDevice(device, DEVICE_UUID);

        final JsonObject request = new JsonObject();
        request.addProperty("type", "status");
        serialDevice.putAsVM(request.toString());
        rpcAdapter.step(0); // process message

        final String message = serialDevice.readMessageAsVM();
        Assertions.assertNotNull(message);

        final JsonObject json = new JsonParser().parse(message).getAsJsonObject();

        final JsonArray devicesJson = json.getAsJsonArray("data");
        Assertions.assertEquals(1, devicesJson.size());

        final JsonObject deviceJson = devicesJson.get(0).getAsJsonObject();

        final JsonArray methodsJson = deviceJson.getAsJsonArray("methods");
        Assertions.assertEquals(1, methodsJson.size());
    }

    @Test
    public void simpleMethod() {
        final VoidIntMethod method = new VoidIntMethod();
        final TestRPCDeviceInterface device = new TestRPCDeviceInterface(method);
        setDevice(device, DEVICE_UUID);

        invokeMethod(DEVICE_UUID, method.getName(), 0xdeadbeef);

        Assertions.assertEquals(0xdeadbeef, method.passedValue);
    }

    @Test
    public void returningMethod() {
        final IntLongMethod method = new IntLongMethod();
        final TestRPCDeviceInterface device = new TestRPCDeviceInterface(method);
        setDevice(device, DEVICE_UUID);

        final JsonElement result = invokeMethod(DEVICE_UUID, method.getName(), 0xdeadbeefcafebabeL);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isJsonPrimitive());
        Assertions.assertEquals(0xcafebabe, result.getAsInt());
    }

    @Test
    public void annotatedObject() {
        final SimpleObject object = new SimpleObject();
        final ObjectDevice device = new ObjectDevice(object);
        setDevice(device, DEVICE_UUID);

        Assertions.assertEquals(42 + 23, invokeMethod(DEVICE_UUID, "add", 42, 23).getAsInt());
    }

    private void setDevice(final RPCDevice device, final UUID deviceId) {
        when(busController.getDevices()).thenReturn(Collections.singleton(device));
        when(busController.getDeviceIdentifiers(device)).thenReturn(Collections.singleton(deviceId));

        // trigger device cache rebuild
        rpcAdapter.pause();
        rpcAdapter.resume();
    }

    private JsonElement invokeMethod(final UUID deviceId, final String name, final Object... parameters) {
        final JsonObject request = new JsonObject();
        request.addProperty("type", "invoke");
        final JsonObject methodInvocation = new JsonObject();
        methodInvocation.addProperty("deviceId", deviceId.toString());
        methodInvocation.addProperty("name", name);
        final JsonArray parametersJson = new JsonArray();
        methodInvocation.add("parameters", parametersJson);
        for (final Object parameter : parameters) {
            parametersJson.add(new Gson().toJson(parameter));
        }
        request.add("data", methodInvocation);
        serialDevice.putAsVM(request.toString());

        rpcAdapter.step(0);

        final String result = serialDevice.readMessageAsVM();
        Assertions.assertNotNull(result);
        final JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
        Assertions.assertEquals("result", resultJson.get("type").getAsString());
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

    private static final class TestRPCDeviceInterface implements RPCDevice {
        private final RPCMethod method;

        public TestRPCDeviceInterface(final RPCMethod method) {
            this.method = method;
        }

        @Override
        public List<String> getTypeNames() {
            return Collections.singletonList(getClass().getSimpleName());
        }

        @Override
        public List<RPCMethod> getMethods() {
            return Collections.singletonList(method);
        }
    }
}
