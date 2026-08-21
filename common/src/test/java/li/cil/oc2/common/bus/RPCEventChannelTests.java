/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.entity.robot.RobotActionCompletedEvent;
import li.cil.oc2.common.entity.robot.RobotActionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class RPCEventChannelTests {
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
        adapter = newAdapter(eventDevice);
    }

    private RPCDeviceBusAdapter newAdapter(final TestSerialDevice events) {
        eventDevice = events;
        devices = new HashSet<>();
        identifiers = new HashMap<>();
        busController = mock(DeviceBusController.class);
        when(busController.getDevices()).thenReturn(devices);
        when(busController.getDeviceIdentifiers(any()))
            .then(invocation -> identifiers.get(invocation.getArgument(0)));
        return new RPCDeviceBusAdapter(serialDevice, blobDevice, events);
    }

    // --------------------------------------------------------------------- //

    @Test
    public void deviceChangeIsAnnounced() {
        addDevice("redstone");
        adapter.resume(busController, true);
        adapter.step(0);

        final JsonObject event = event();
        assertEquals("devicesChanged", event.get("type").getAsString());
        assertEquals(request("list").get("gen").getAsInt(), event.get("gen").getAsInt(),
            "the event must report the generation the next reply will report");
    }

    @Test
    public void nothingIsAnnouncedWhenNothingChanged() {
        addDevice("redstone");
        adapter.resume(busController, true);
        adapter.step(0);
        eventDevice.drainAsVM();

        adapter.resume(busController, false);
        adapter.step(0);

        assertNull(eventDevice.readMessageAsVM(), "a resume that changed nothing must be silent");
    }

    @Test
    public void everyChangeIsAnnouncedInOrder() {
        addDevice("redstone");
        adapter.resume(busController, true);
        addDevice("inventory");
        adapter.resume(busController, true);
        addDevice("energy");
        adapter.resume(busController, true);
        adapter.step(0);

        int previous = 0;
        for (int i = 0; i < 3; i++) {
            final JsonObject event = event();
            assertEquals("devicesChanged", event.get("type").getAsString());
            final int gen = event.get("gen").getAsInt();
            assertTrue(gen > previous, "generations must arrive in order, got " + gen + " after " + previous);
            previous = gen;
        }

        assertNull(eventDevice.readMessageAsVM(), "exactly three changes, exactly three events");
        assertEquals(previous, request("list").get("gen").getAsInt(),
            "the last event must carry the generation the next reply reports");
    }

    @Test
    public void theQueueIsBoundedWhenNobodyReads() {
        final TestSerialDevice deafEvents = new TestSerialDevice(0);
        final RPCEventChannel channel = new RPCEventChannel(deafEvents);
        final byte[] kilobyte = new byte[Constants.KILOBYTE];

        int accepted = 0;
        while (channel.addEvent(RPCMessageChannel.frame(kilobyte))) {
            accepted++;
            assertTrue(accepted <= 16, "the queue accepted " + accepted + " KiB and kept going");
        }

        final TestSerialDevice reader = new TestSerialDevice();
        final RPCEventChannel drained = new RPCEventChannel(reader);
        while (drained.addEvent(RPCMessageChannel.frame(kilobyte))) {
            // fill it
        }
        drained.flush();
        assertTrue(reader.drainAsVM().length > 0, "precondition: the queue drained");
        assertTrue(drained.addEvent(RPCMessageChannel.frame(kilobyte)),
            "the queue never recovered after being drained");
    }

    @Test
    public void theChannelReportsWhatItRefused() {
        final TestSerialDevice deaf = new TestSerialDevice(0);
        final RPCEventChannel channel = new RPCEventChannel(deaf);
        final byte[] kilobyte = new byte[Constants.KILOBYTE];

        while (channel.addEvent(RPCMessageChannel.frame(kilobyte))) {
            // fill it
        }
        channel.addEvent(RPCMessageChannel.frame(kilobyte));

        assertTrue(channel.takeDropped() > 0, "refusing an event went unreported");
        assertEquals(0, channel.takeDropped(), "taking the count must clear it");
    }

    @Test
    public void onlyOneNoticeIsOutstandingAtATime() {
        final TestSerialDevice deaf = new TestSerialDevice(0);
        final RPCEventChannel channel = new RPCEventChannel(deaf);
        final byte[] kilobyte = new byte[Constants.KILOBYTE];

        while (channel.addEvent(RPCMessageChannel.frame(kilobyte))) {
            // fill it
        }
        assertTrue(channel.takeDropped() > 0, "precondition: something was refused");

        channel.addNotice(RPCMessageChannel.frame("notice".getBytes(StandardCharsets.UTF_8)));

        channel.addEvent(RPCMessageChannel.frame(kilobyte));
        assertEquals(0, channel.takeDropped(),
            "a second notice would have queued up after the first");
    }

    @Test
    public void refusalsBehindAPendingNoticeAreNotForgotten() {
        final TestSerialDevice reader = new TestSerialDevice();
        final RPCEventChannel channel = new RPCEventChannel(reader);
        final byte[] kilobyte = new byte[Constants.KILOBYTE];

        while (channel.addEvent(RPCMessageChannel.frame(kilobyte))) {
            // fill it
        }
        assertTrue(channel.takeDropped() > 0, "precondition: something was refused");
        channel.addNotice(RPCMessageChannel.frame("notice".getBytes(StandardCharsets.UTF_8)));

        channel.addEvent(RPCMessageChannel.frame(kilobyte));
        assertEquals(0, channel.takeDropped(), "a second notice queued up after the first");

        channel.flush();
        assertTrue(channel.takeDropped() > 0,
            "a refusal while the notice was pending was never reported");
    }

    @Test
    public void droppedEventsAreAnnouncedToTheGuest() {
        final TestSerialDevice slow = new TestSerialDevice(256); // room for a frame, not for many
        adapter = newAdapter(slow);
        addDevice("redstone");
        adapter.resume(busController, true);

        boolean refused = false;
        for (int i = 0; i < 4000 && !refused; i++) {
            refused = !adapter.addEvent(RobotActionCompletedEvent.TYPE,
                new RobotActionCompletedEvent(i, RobotActionResult.SUCCESS));
            adapter.step(0);
        }
        assertTrue(refused, "precondition: the queue never filled up");

        JsonObject notice = null;
        for (int i = 0; i < 40000 && notice == null; i++) {
            adapter.step(0);
            final String message = slow.readMessageAsVM();
            if (message != null) {
                final JsonObject parsed = JsonParser.parseString(message).getAsJsonObject();
                if ("eventsDropped".equals(parsed.get("type").getAsString())) {
                    notice = parsed;
                }
            }
        }

        assertNotNull(notice, "the guest was never told it had missed events");
        assertTrue(notice.get("data").getAsInt() > 0, "the notice must say how many were lost");
        assertEquals(request("list").get("gen").getAsInt(), notice.get("gen").getAsInt(),
            "the notice must carry the generation, since that is what corrects the guest");
    }

    @Test
    public void eventsNeverAppearOnTheRpcChannel() {
        addDevice("redstone");
        adapter.resume(busController, true);
        adapter.step(0);

        assertNull(serialDevice.readMessageAsVM(), "an unsolicited message reached the RPC port");
        assertNotNull(eventDevice.readMessageAsVM());
    }

    @Test
    public void guestThatNeverReadsEventsDoesNotStallTheBus() {
        final TestSerialDevice deafEvents = new TestSerialDevice(0);
        adapter = newAdapter(deafEvents);
        addDevice("redstone");
        adapter.resume(busController, true);

        for (int i = 0; i < 500; i++) {
            adapter.resume(busController, true);
            adapter.step(0);
        }

        assertEquals(0, deafEvents.drainAsVM().length, "nothing should have been accepted");

        assertEquals("list", request("list").get("type").getAsString(),
            "an undeliverable event blocked the RPC channel");
    }

    @Test
    public void resetDropsAPendingEvent() {
        addDevice("redstone");
        adapter.resume(busController, true);

        // Before any step, so the event is still queued rather than already written.
        adapter.reset();
        adapter.step(0);

        assertEquals(0, eventDevice.drainAsVM().length, "a queued event survived a reset");
    }

    @Test
    public void resetDropsAHalfWrittenEvent() {
        final TestSerialDevice trickle = new TestSerialDevice(4);
        adapter = newAdapter(trickle);
        addDevice("redstone");
        adapter.resume(busController, true);
        adapter.step(0);

        assertTrue(trickle.drainAsVM().length > 0, "precondition: some of the frame went out");

        adapter.reset();
        for (int i = 0; i < 10; i++) {
            adapter.step(0);
        }

        assertEquals(0, trickle.drainAsVM().length, "the rest of the frame was still written");
    }

    @Test
    public void eventsUseTheSameFramingAsReplies() {
        addDevice("redstone");
        adapter.resume(busController, true);
        adapter.step(0);

        final byte[] raw = eventDevice.drainAsVM();
        assertEquals(0, raw[0], "frames start with a delimiter");
        assertEquals(0, raw[raw.length - 1], "frames end with a delimiter");
    }

    @Test
    public void slowGuestStillGetsTheEventOnceItReads() {
        // One byte at a time: the frame cannot go out in a single step.
        final TestSerialDevice trickle = new TestSerialDevice(1);
        adapter = newAdapter(trickle);
        addDevice("redstone");
        adapter.resume(busController, true);

        final StringBuilder message = new StringBuilder();
        for (int i = 0; i < 4096; i++) {
            adapter.step(0);
            final byte[] chunk = trickle.drainAsVM();
            for (final byte b : chunk) {
                if (b != 0) {
                    message.append((char) b);
                }
            }
            if (message.indexOf("devicesChanged") >= 0) {
                break;
            }
        }

        assertTrue(message.indexOf("devicesChanged") >= 0,
            "a one-byte-at-a-time reader never received the event");
    }

    @Test
    public void guestWritesToTheEventPortAreDiscarded() {
        eventDevice.putRawAsVM("nonsense from a confused guest".getBytes(StandardCharsets.UTF_8));

        addDevice("redstone");
        adapter.resume(busController, true);
        adapter.step(0);

        assertEquals(-1, eventDevice.read(), "the guest's bytes were left sitting in the queue");
        assertEquals("devicesChanged", event().get("type").getAsString(),
            "the channel must still work after a guest wrote to it");
    }

    @Test
    public void deviceEventCarriesItsPayload() {
        assertTrue(adapter.addEvent(RobotActionCompletedEvent.TYPE,
            new RobotActionCompletedEvent(7, RobotActionResult.FAILURE)));
        adapter.step(0);

        final JsonObject event = event();
        assertEquals(RobotActionCompletedEvent.TYPE, event.get("type").getAsString());

        final JsonObject data = event.getAsJsonObject("data");
        assertEquals(7, data.get("actionId").getAsInt());
        assertEquals("FAILURE", data.get("result").getAsString(),
            "the guest compares the result by name");
    }

    @Test
    public void eventsRaisedFromAnotherThreadArriveIntactAndInOrder() throws InterruptedException {
        // Robot actions complete on the server thread while the VM worker drains the channel.
        final int count = 500;
        final List<Integer> seen = new ArrayList<>();

        final Thread producer = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                while (!adapter.addEvent(RobotActionCompletedEvent.TYPE,
                    new RobotActionCompletedEvent(i, RobotActionResult.SUCCESS))) {
                    Thread.onSpinWait(); // queue is full; let the reader catch up
                }
            }
        }, "event-producer");
        producer.start();

        try {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (seen.size() < count && System.nanoTime() < deadline) {
                adapter.step(0);

                String message;
                while ((message = eventDevice.readMessageAsVM()) != null) {
                    final JsonObject event = JsonParser.parseString(message).getAsJsonObject();
                    if (!RobotActionCompletedEvent.TYPE.equals(event.get("type").getAsString())) {
                        continue;
                    }
                    seen.add(event.getAsJsonObject("data").get("actionId").getAsInt());
                }
            }
        } finally {
            producer.join(TimeUnit.SECONDS.toMillis(30));
        }

        assertEquals(count, seen.size(), "events were lost or the reader timed out");
        for (int i = 0; i < count; i++) {
            assertEquals(i, seen.get(i), "frames were interleaved or reordered at index " + i);
        }
    }

    // --------------------------------------------------------------------- //

    private JsonObject event() {
        final String message = eventDevice.readMessageAsVM();
        assertNotNull(message, "no event was pushed");
        return JsonParser.parseString(message).getAsJsonObject();
    }

    private JsonObject request(final String type) {
        serialDevice.putAsVM("{\"type\":\"" + type + "\"}");
        adapter.step(0);
        final String message = serialDevice.readMessageAsVM();
        assertNotNull(message, "no reply to a " + type + " request");
        return JsonParser.parseString(message).getAsJsonObject();
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
