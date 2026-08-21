/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class RPCSynchronizedCallTests {
    private TestSerialDevice serialDevice;
    private RPCDeviceBusAdapter adapter;
    private UUID deviceId;
    private Counter counter;

    @BeforeEach
    public void setupEach() {
        serialDevice = new TestSerialDevice();
        adapter = new RPCDeviceBusAdapter(serialDevice, new TestSerialDevice(), new TestSerialDevice());

        counter = new Counter();
        final RPCDevice device = new ObjectDevice(counter, "counter");
        deviceId = UUID.randomUUID();
        final Set<Device> devices = new HashSet<>();
        devices.add(device);
        final Map<Device, Set<UUID>> identifiers = new HashMap<>();
        identifiers.put(device, Set.of(deviceId));

        final DeviceBusController busController = mock(DeviceBusController.class);
        when(busController.getDevices()).thenReturn(devices);
        when(busController.getDeviceIdentifiers(any()))
            .then(invocation -> identifiers.get(invocation.getArgument(0)));
        adapter.resume(busController, true);
    }

    // --------------------------------------------------------------------- //

    @Test
    public void aSynchronizedCallIsAnsweredOnlyOnTheNextTick() {
        serialDevice.putAsVM(invocation("bump"));
        adapter.step(0);

        assertEquals(0, counter.bumps, "a synchronized callback ran on the VM worker");
        assertNull(serialDevice.readMessageAsVM(), "answered before the server thread ran it");

        adapter.tick();
        assertEquals(1, counter.bumps);

        adapter.step(0);
        assertEquals("result", reply().get("type").getAsString());
    }

    @Test
    public void noFurtherRequestIsTakenWhileACallIsPending() {
        serialDevice.putAsVM(invocation("bump"));
        serialDevice.putAsVM(invocation("bump"));
        adapter.step(0);
        adapter.step(0);

        adapter.tick();
        assertEquals(1, counter.bumps, "the second request was dispatched while the first was pending");

        adapter.step(0); // writes the first reply
        adapter.step(0); // reads the second request, schedules it
        adapter.tick();
        assertEquals(2, counter.bumps, "the second request was never picked up");
    }

    @Test
    public void theReplyIsWrittenBeforeTheNextCallIsAccepted() {
        serialDevice.putAsVM(invocation("bump"));
        adapter.step(0);
        adapter.tick();

        serialDevice.putAsVM(invocation("bump"));
        adapter.step(0);

        assertEquals("result", reply().get("type").getAsString(), "the first reply was lost");
        assertNull(serialDevice.readMessageAsVM(), "two replies came back for one dispatched call");
    }

    @Test
    public void anUnsynchronizedCallDoesNotWaitForATick() {
        serialDevice.putAsVM(invocation("bumpNow"));
        adapter.step(0);

        assertEquals(1, counter.bumps);
        assertEquals("result", reply().get("type").getAsString());
    }

    // --------------------------------------------------------------------- //

    private String invocation(final String method) {
        return "{\"type\":\"invoke\",\"data\":{\"deviceId\":\"" + deviceId
            + "\",\"name\":\"" + method + "\",\"parameters\":[]}}";
    }

    private JsonObject reply() {
        final String message = serialDevice.readMessageAsVM();
        assertNotNull(message, "no reply");
        return JsonParser.parseString(message).getAsJsonObject();
    }

    public static final class Counter {
        public int bumps;

        @Callback
        public int bump() {
            return ++bumps;
        }

        @Callback(synchronize = false)
        public int bumpNow() {
            return ++bumps;
        }
    }
}
