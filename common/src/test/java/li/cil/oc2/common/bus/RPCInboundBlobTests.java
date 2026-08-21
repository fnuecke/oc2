/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class RPCInboundBlobTests {
    private static final byte[] PAYLOAD = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
    private static final int CHECKSUM = 746590911;

    private TestSerialDevice serialDevice;
    private TestSerialDevice blobDevice;
    private TestSerialDevice eventDevice;
    private RPCDeviceBusAdapter adapter;
    private UUID deviceId;
    private BlobSink sink;
    private DeviceBusController busController;

    @BeforeEach
    public void setupEach() {
        serialDevice = new TestSerialDevice();
        blobDevice = new TestSerialDevice();
        eventDevice = new TestSerialDevice();
        adapter = new RPCDeviceBusAdapter(serialDevice, blobDevice, eventDevice);

        sink = new BlobSink();
        final RPCDevice device = new ObjectDevice(sink, "blobs");
        deviceId = UUID.randomUUID();
        final Set<Device> devices = new HashSet<>();
        devices.add(device);
        final Map<Device, Set<UUID>> identifiers = new HashMap<>();
        identifiers.put(device, Set.of(deviceId));

        busController = mock(DeviceBusController.class);
        when(busController.getDevices()).thenReturn(devices);
        when(busController.getDeviceIdentifiers(any()))
            .then(invocation -> identifiers.get(invocation.getArgument(0)));
        adapter.resume(busController, true);
    }

    // --------------------------------------------------------------------- //

    @Test
    public void payloadReachesTheCallback() {
        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);

        assertArrayEquals(PAYLOAD, sink.received, "the callback did not get the payload");
        assertEquals(1, sink.calls);
        assertEquals("result", reply().get("type").getAsString());
    }

    @Test
    public void aCorruptedPayloadIsRefusedAndTheChannelKeepsWorking() {
        final byte[] corrupted = PAYLOAD.clone();
        corrupted[3] ^= 0x01;

        blobDevice.putRawAsVM(corrupted);
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);

        assertNull(sink.received, "a corrupted payload must not reach the callback");
        assertEquals(RPCDeviceBusAdapter.ERROR_PAYLOAD_CORRUPT, reply().get("data").getAsString());

        // And the next call still works, so a bad payload is not fatal to the channel.
        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);
        assertArrayEquals(PAYLOAD, sink.received);
    }

    @Test
    public void anImplausibleLengthIsRefused() {
        serialDevice.putAsVM(invocation(Integer.MAX_VALUE, 0));
        adapter.step(0);

        assertEquals(RPCDeviceBusAdapter.ERROR_PAYLOAD_MISMATCH, reply().get("data").getAsString());
        assertNull(sink.received);
    }

    @Test
    public void payloadHandedOverAcrossSeveralStepsBeforeTheMessage() {
        blobDevice.putRawAsVM(new byte[]{PAYLOAD[0], PAYLOAD[1]});
        adapter.step(0);

        final byte[] rest = new byte[PAYLOAD.length - 2];
        System.arraycopy(PAYLOAD, 2, rest, 0, rest.length);
        blobDevice.putRawAsVM(rest);
        adapter.step(0);

        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);

        assertArrayEquals(PAYLOAD, sink.received);
        assertEquals("result", reply().get("type").getAsString());
    }

    @Test
    public void anUnderSentPayloadDoesNotWedgeTheBus() {
        blobDevice.putRawAsVM(new byte[]{PAYLOAD[0]});
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);

        assertNull(sink.received);
        assertEquals(RPCDeviceBusAdapter.ERROR_PAYLOAD_MISMATCH, reply().get("data").getAsString());

        // An unrelated request is still answered, which is the point.
        serialDevice.putAsVM("{\"type\":\"list\"}");
        adapter.step(0);
        assertEquals("list", reply().get("type").getAsString(), "the bus stopped answering");
    }

    @Test
    public void anOverSentPayloadIsRefusedAndTheChannelRecovers() {
        final byte[] tooMuch = new byte[PAYLOAD.length + 3];
        System.arraycopy(PAYLOAD, 0, tooMuch, 0, PAYLOAD.length);
        blobDevice.putRawAsVM(tooMuch);
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);

        assertNull(sink.received);
        assertEquals(RPCDeviceBusAdapter.ERROR_PAYLOAD_MISMATCH, reply().get("data").getAsString());

        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);
        assertArrayEquals(PAYLOAD, sink.received, "the channel did not recover");
    }

    @Test
    public void aPayloadOnANonInvocationIsRefused() {
        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM("{\"type\":\"list\",\"blob\":{\"length\":" + PAYLOAD.length
            + ",\"checksum\":" + CHECKSUM + "}}");
        adapter.step(0);

        assertEquals(RPCDeviceBusAdapter.ERROR_MALFORMED_MESSAGE, reply().get("data").getAsString());
    }

    @Test
    public void aZeroLengthPayloadIsAccepted() {
        serialDevice.putAsVM(invocation(0, 0));
        adapter.step(0);

        assertNotNull(sink.received);
        assertEquals(0, sink.received.length);
    }

    @Test
    public void twoMarkersInOneCallAreRefused() {
        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM("{\"type\":\"invoke\",\"blob\":{\"length\":" + PAYLOAD.length
            + ",\"checksum\":" + CHECKSUM + "},\"data\":{\"deviceId\":\"" + deviceId
            + "\",\"name\":\"writeTwo\",\"parameters\":[{\"$blob\":true},{\"$blob\":true}]}}");
        adapter.step(0);

        assertEquals("error", reply().get("type").getAsString());
        assertNull(sink.received, "one payload must not be handed to two parameters");
    }

    @Test
    public void aBinaryParameterOnASynchronizedCallbackIsRefusedClearly() {
        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM("{\"type\":\"invoke\",\"blob\":{\"length\":" + PAYLOAD.length
            + ",\"checksum\":" + CHECKSUM + "},\"data\":{\"deviceId\":\"" + deviceId
            + "\",\"name\":\"writeSynchronized\",\"parameters\":[{\"$blob\":true}]}}");
        adapter.step(0);

        assertEquals(RPCDeviceBusAdapter.ERROR_PAYLOAD_NEEDS_UNSYNCHRONIZED,
            reply().get("data").getAsString(),
            "the payload is gone by the time a synchronized call runs, so say so");
    }

    @Test
    public void aMalformedPayloadReferenceIsRefused() {
        for (final String blob : new String[]{"{\"checksum\":0}", "{\"length\":\"x\",\"checksum\":0}", "5"}) {
            setupEach();
            serialDevice.putAsVM("{\"type\":\"invoke\",\"blob\":" + blob
                + ",\"data\":{\"deviceId\":\"" + deviceId
                + "\",\"name\":\"writeBlob\",\"parameters\":[{\"$blob\":true}]}}");
            assertDoesNotThrow(() -> adapter.step(0), "blob reference " + blob + " escaped as an exception");
            assertNull(sink.received, "blob reference " + blob + " was accepted");
            assertEquals("error", reply().get("type").getAsString(),
                "blob reference " + blob + " got no reply, so a guest would wait forever");
        }
    }

    @Test
    public void anInlineByteArrayParameterIsRefused() {
        serialDevice.putAsVM("{\"type\":\"invoke\",\"data\":{\"deviceId\":\"" + deviceId
            + "\",\"name\":\"writeBlob\",\"parameters\":[[1,2,3]]}}");
        adapter.step(0);

        assertNull(sink.received);
        assertEquals("error", reply().get("type").getAsString());
    }

    @Test
    public void aMarkerWithNoPayloadIsRefused() {
        serialDevice.putAsVM("{\"type\":\"invoke\",\"data\":{\"deviceId\":\"" + deviceId
            + "\",\"name\":\"writeBlob\",\"parameters\":[{\"$blob\":true}]}}");
        adapter.step(0);

        assertNull(sink.received, "a marker without a payload must not reach the callback");
        assertEquals("error", reply().get("type").getAsString());
    }

    @Test
    public void resetAbandonsAPartiallyReceivedPayload() {
        blobDevice.putRawAsVM(new byte[]{PAYLOAD[0]});
        adapter.step(0);

        adapter.reset();

        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);
        assertArrayEquals(PAYLOAD, sink.received);
    }

    @Test
    public void payloadLargerThanTheChannelStillGetsThrough() {
        final byte[] large = new byte[4096];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i * 31 + 7);
        }

        serialDevice = new TestSerialDevice();
        blobDevice = new TestSerialDevice(Integer.MAX_VALUE, 64);
        adapter = new RPCDeviceBusAdapter(serialDevice, blobDevice, eventDevice);
        adapter.resume(busController, true);

        int offered = 0;
        for (int i = 0; i < 1000 && offered < large.length; i++) {
            offered = blobDevice.offerRawAsVM(large, offered);
            adapter.step(0);
        }
        assertEquals(large.length, offered, "the guest never got the whole payload out");

        serialDevice.putAsVM(invocation(large.length, RPCPayloadChannel.checksum(large)));
        adapter.step(0);

        assertArrayEquals(large, sink.received);
    }

    @Test
    public void abandonedPayloadBytesDoNotPoisonTheNextTransfer() {
        blobDevice.putRawAsVM("stray bytes nobody claimed".getBytes(StandardCharsets.UTF_8));

        serialDevice.putAsVM("{\"type\":\"list\"}");
        adapter.step(0);
        assertEquals("list", reply().get("type").getAsString());

        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);
        assertArrayEquals(PAYLOAD, sink.received, "stale bytes broke the next transfer");
    }

    @Test
    public void aPayloadPastTheCapIsRefusedAndTheChannelRecovers() {
        final byte[] tooMuch = new byte[Constants.RPC_MAX_PAYLOAD_SIZE + 1];
        blobDevice.putRawAsVM(tooMuch);
        adapter.step(0);

        assertEquals(-1, blobDevice.read(), "the host must drain even what it cannot keep");

        serialDevice.putAsVM(invocation(0, 0));
        adapter.step(0);
        assertNull(sink.received, "an overflowed transfer must not pass as an empty one");
        assertEquals(RPCDeviceBusAdapter.ERROR_PAYLOAD_MISMATCH, reply().get("data").getAsString());

        blobDevice.putRawAsVM(PAYLOAD);
        serialDevice.putAsVM(invocation(PAYLOAD.length, CHECKSUM));
        adapter.step(0);
        assertArrayEquals(PAYLOAD, sink.received, "the channel did not recover after an overflow");
    }

    @Test
    public void payloadThatExactlyFillsTheChannelIsAccepted() {
        final byte[] exact = new byte[Constants.RPC_MAX_PAYLOAD_SIZE];
        for (int i = 0; i < exact.length; i++) {
            exact[i] = (byte) (i * 31 + 7);
        }

        blobDevice.putRawAsVM(exact);
        serialDevice.putAsVM(invocation(exact.length, RPCPayloadChannel.checksum(exact)));
        adapter.step(0);

        assertArrayEquals(exact, sink.received, "a payload that fits exactly is not an oversized one");
    }

    // --------------------------------------------------------------------- //

    private String invocation(final int length, final int checksum) {
        return "{\"type\":\"invoke\",\"blob\":{\"length\":" + length + ",\"checksum\":" + checksum + "},"
            + "\"data\":{\"deviceId\":\"" + deviceId
            + "\",\"name\":\"writeBlob\",\"parameters\":[{\"$blob\":true}]}}";
    }

    private JsonObject reply() {
        final String message = serialDevice.readMessageAsVM();
        assertNotNull(message, "no reply");
        return JsonParser.parseString(message).getAsJsonObject();
    }

    public static final class BlobSink {
        public byte[] received;
        public int calls;

        @Callback(synchronize = false)
        public int writeBlob(@Parameter("data") final byte[] data) {
            received = data;
            calls++;
            return data.length;
        }

        @Callback(synchronize = false)
        public int writeTwo(@Parameter("a") final byte[] a, @Parameter("b") final byte[] b) {
            received = a;
            calls++;
            return a.length + b.length;
        }

        @Callback
        public int writeSynchronized(@Parameter("data") final byte[] data) {
            received = data;
            calls++;
            return data.length;
        }
    }
}
