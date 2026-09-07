/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.ceres.BinarySerialization;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.sedna.Sedna;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public final class RPCBusGenerationTests {
    private TestSerialDevice serialDevice;
    private TestSerialDevice blobDevice;
    private TestSerialDevice eventDevice;
    private DeviceBusController busController;
    private RPCDeviceBusAdapter adapter;
    private Set<Device> devices;
    private Map<Device, Set<UUID>> identifiers;

    @BeforeEach
    public void setupEach() {
        serialDevice = new TestSerialDevice();
        blobDevice = new TestSerialDevice();
        eventDevice = new TestSerialDevice();
        adapter = new RPCDeviceBusAdapter(serialDevice, blobDevice, eventDevice);
        devices = new HashSet<>();
        identifiers = new HashMap<>();
        busController = mock(DeviceBusController.class);
        when(busController.getDevices()).thenReturn(devices);
        when(busController.getDeviceIdentifiers(any()))
            .then(invocation -> identifiers.get(invocation.getArgument(0)));
    }

    @Test
    public void listReplyCarriesGeneration() {
        addDevice("redstone");
        adapter.resume(busController);

        final int gen = request("list").get("gen").getAsInt();
        assertEquals(gen, request("list").get("gen").getAsInt(),
            "repeated list replies must report the same generation");
    }

    @Test
    public void everyReplyKindCarriesGeneration() {
        addDevice("redstone");
        adapter.resume(busController);
        final int expected = generation();

        final JsonObject error = request("invoke", "\"data\":{\"deviceId\":\""
            + UUID.randomUUID() + "\",\"name\":\"nope\",\"parameters\":[]}");
        assertEquals("error", error.get("type").getAsString());
        assertEquals(expected, error.get("gen").getAsInt(), "error reply carried the wrong generation");

        final JsonObject methods = request("methods", "\"data\":\"" + onlyIdentifier() + "\"");
        assertEquals(expected, methods.get("gen").getAsInt(), "methods reply carried the wrong generation");
    }

    @Test
    public void generationMovesWhenDevicesChange() {
        addDevice("redstone");
        adapter.resume(busController);
        final int before = generation();

        addDevice("inventory");
        adapter.resume(busController);

        assertNotEquals(before, generation(), "generation did not move after the device set changed");
    }

    @Test
    public void generationNeverGoesBackwards() {
        addDevice("redstone");
        adapter.resume(busController);
        int previous = generation();

        for (int i = 0; i < 5; i++) {
            addDevice("device" + i);
            adapter.resume(busController);
            final int current = generation();
            assertTrue(current > previous,
                "generation must increase monotonically, went " + previous + " -> " + current);
            previous = current;
        }
    }

    @Test
    public void generationAndDeviceListAgreeInTheSameReply() {
        addDevice("redstone");
        adapter.resume(busController);
        final int before = generation();

        addDevice("inventory");
        adapter.resume(busController);

        final JsonObject reply = request("list");
        assertNotEquals(before, reply.get("gen").getAsInt());
        assertEquals(2, reply.getAsJsonArray("data").size(),
            "new generation was published with a stale device list");
    }

    @Test
    public void resetDoesNotRewindGeneration() {
        addDevice("redstone");
        adapter.resume(busController);
        final int before = generation();

        adapter.reset();

        assertEquals(before, generation(), "reset must leave the generation exactly where it was");
    }


    @Test
    public void resultReplyCarriesGeneration() {
        addDevice("redstone");
        adapter.resume(busController);
        final int expected = generation();

        final JsonObject result = request("invoke", "\"data\":{\"deviceId\":\""
            + onlyIdentifier() + "\",\"name\":\"ping\",\"parameters\":[]}");
        assertEquals("result", result.get("type").getAsString(), result.toString());
        assertEquals(expected, result.get("gen").getAsInt(),
            "result reply reported a different generation than list");
    }

    @Test
    public void failedRebuildNeitherWedgesTheAdapterNorMovesTheGeneration() {
        addDevice("redstone");
        adapter.resume(busController);
        final int before = generation();

        adapter.pause();
        doThrow(new IllegalStateException("provider blew up")).when(busController).getDevices();
        assertThrows(IllegalStateException.class, () -> adapter.resume(busController));

        doReturn(devices).when(busController).getDevices();
        assertEquals("list", request("list").get("type").getAsString(),
            "the adapter stayed paused after a failed rebuild, so the machine is wedged");
        assertEquals(before, generation(),
            "generation moved even though the rebuild failed; guests would cache a list that was never published");
    }

    @Test
    public void generationSurvivesSaveAndLoad() {
        Sedna.initialize();

        addDevice("redstone");
        adapter.resume(busController);
        addDevice("inventory");
        adapter.resume(busController);
        final int saved = generation();
        assertTrue(saved > 0, "precondition: the counter has moved off its initial value");

        final ByteBuffer data = BinarySerialization.serialize(adapter);

        final TestSerialDevice restoredSerial = new TestSerialDevice();
        final RPCDeviceBusAdapter restored = new RPCDeviceBusAdapter(
            restoredSerial, new TestSerialDevice(), new TestSerialDevice());
        BinarySerialization.deserialize(data, restored);

        restoredSerial.putAsVM("{\"type\":\"list\"}");
        restored.step(0);
        final String reply = restoredSerial.readMessageAsVM();
        assertNotNull(reply, "the restored adapter answered nothing");
        assertEquals(saved, JsonParser.parseString(reply).getAsJsonObject().get("gen").getAsInt(),
            "the generation did not survive the round trip");
    }

    // --------------------------------------------------------------------- //

    private int generation() {
        return request("list").get("gen").getAsInt();
    }

    private JsonObject request(final String type) {
        return request(type, null);
    }

    private JsonObject request(final String type, final String extra) {
        serialDevice.putAsVM("{\"type\":\"" + type + "\""
            + (extra == null ? "" : "," + extra) + "}");
        adapter.step(0);
        final String message = serialDevice.readMessageAsVM();
        assertNotNull(message, "no reply to a " + type + " request");
        return JsonParser.parseString(message).getAsJsonObject();
    }

    private UUID onlyIdentifier() {
        return identifiers.values().iterator().next().iterator().next();
    }

    private void addDevice(final String typeName) {
        final RPCDevice device = new ObjectDevice(new Pingable(), typeName);
        devices.add(device);
        final Set<UUID> ids = new HashSet<>();
        ids.add(UUID.randomUUID());
        identifiers.put(device, ids);
    }

    public static final class Pingable {
        @Callback(synchronize = false)
        public int ping() {
            return 1;
        }
    }
}
