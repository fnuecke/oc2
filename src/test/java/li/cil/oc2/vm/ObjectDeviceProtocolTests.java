package li.cil.oc2.vm;

import com.google.gson.*;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.device.DeviceMethod;
import li.cil.oc2.api.device.IdentifiableDevice;
import li.cil.oc2.api.device.object.Callback;
import li.cil.oc2.api.device.object.ObjectDevice;
import li.cil.oc2.api.device.object.Parameter;
import li.cil.oc2.common.bus.DeviceBusControllerImpl;
import li.cil.oc2.common.bus.DeviceBusElementImpl;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.device.IdentifiableDeviceImpl;
import li.cil.sedna.api.device.serial.SerialDevice;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectDeviceProtocolTests {
    private static final BlockPos CONTROLLER_POS = new BlockPos(0, 0, 0);

    @Mock private Capability<DeviceBusElement> busElementCapability;
    private World world;
    private TestSerialDevice serialDevice;
    private DeviceBusControllerImpl controller;
    private DeviceBusElement busElement;

    @BeforeEach
    public void setupEach() {
        MockitoAnnotations.initMocks(this);
        Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY = busElementCapability;

        serialDevice = new TestSerialDevice();
        controller = new DeviceBusControllerImpl(serialDevice);
        busElement = new DeviceBusElementImpl();

        world = mock(World.class);
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);

        final TileEntity tileEntity = mock(TileEntity.class);
        when(world.getTileEntity(any())).thenReturn(tileEntity);

        when(tileEntity.getCapability(eq(busElementCapability), any())).thenReturn(LazyOptional.of(() -> busElement));
    }

    @Test
    public void resetAndReadDescriptor() {
        final VoidIntMethod method = new VoidIntMethod();

        busElement.addDevice(new TestDevice(method));
        controller.scan(world, CONTROLLER_POS);

        final JsonObject request = new JsonObject();
        request.addProperty("type", "status");
        serialDevice.putAsVM(request.toString());
        controller.step(0); // process message

        final String message = serialDevice.readMessageAsVM();
        Assertions.assertNotNull(message);

        final JsonObject json = new JsonParser().parse(message).getAsJsonObject();

        final JsonArray devices = json.getAsJsonArray("data");
        Assertions.assertEquals(1, devices.size());

        final JsonObject device = devices.get(0).getAsJsonObject();

        final JsonArray methods = device.getAsJsonArray("methods");
        Assertions.assertEquals(1, methods.size());
    }

    @Test
    public void simpleMethod() {
        final VoidIntMethod method = new VoidIntMethod();
        final TestDevice device = new TestDevice(method);

        busElement.addDevice(device);
        controller.scan(world, CONTROLLER_POS);

        invokeMethod(device, method.getName(), 0xdeadbeef);

        Assertions.assertEquals(0xdeadbeef, method.passedValue);
    }

    @Test
    public void returningMethod() {
        final IntLongMethod method = new IntLongMethod();
        final TestDevice device = new TestDevice(method);

        busElement.addDevice(device);
        controller.scan(world, CONTROLLER_POS);

        final JsonElement result = invokeMethod(device, method.getName(), 0xdeadbeefcafebabeL);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isJsonPrimitive());
        Assertions.assertEquals(0xcafebabe, result.getAsInt());
    }

    @Test
    public void annotatedObject() {
        final SimpleObject object = new SimpleObject();
        final ObjectDevice device = new ObjectDevice(object);
        final IdentifiableDeviceImpl identifiableDevice = new IdentifiableDeviceImpl(LazyOptional.of(() -> device), UUID.randomUUID());

        busElement.addDevice(identifiableDevice);
        controller.scan(world, CONTROLLER_POS);

        Assertions.assertEquals(42 + 23, invokeMethod(identifiableDevice, "add", 42, 23).getAsInt());
    }

    private JsonElement invokeMethod(final IdentifiableDevice device, final String name, final Object... parameters) {
        final JsonObject request = new JsonObject();
        request.addProperty("type", "invoke");
        final JsonObject methodInvocation = new JsonObject();
        methodInvocation.addProperty("deviceId", device.getUniqueId().toString());
        methodInvocation.addProperty("name", name);
        final JsonArray parametersJson = new JsonArray();
        methodInvocation.add("parameters", parametersJson);
        for (final Object parameter : parameters) {
            parametersJson.add(new Gson().toJson(parameter));
        }
        request.add("data", methodInvocation);
        serialDevice.putAsVM(request.toString());

        controller.step(0);

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
                return new String(bytes.toByteArray());
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

    private static final class TestDevice implements IdentifiableDevice {
        private static final UUID UUID = java.util.UUID.randomUUID();

        private final DeviceMethod method;

        public TestDevice(final DeviceMethod method) {
            this.method = method;
        }

        @Override
        public List<String> getTypeNames() {
            return Collections.singletonList(getClass().getSimpleName());
        }

        @Override
        public List<DeviceMethod> getMethods() {
            return Collections.singletonList(method);
        }

        @Override
        public UUID getUniqueId() {
            return UUID;
        }
    }
}
