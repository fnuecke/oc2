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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class RPCBlobChannelTests {
    private static final byte[] PAYLOAD = "the quick brown fox".getBytes(StandardCharsets.UTF_8);

    private TestSerialDevice serialDevice;
    private TestSerialDevice blobDevice;
    private TestSerialDevice eventDevice;
    private RPCDeviceBusAdapter adapter;
    private UUID deviceId;

    @BeforeEach
    public void setupEach() {
        setUpWith(new TestSerialDevice(), new TestSerialDevice());
    }

    private void setUpWith(final TestSerialDevice rpc, final TestSerialDevice blob) {
        serialDevice = rpc;
        blobDevice = blob;
        eventDevice = new TestSerialDevice();
        adapter = new RPCDeviceBusAdapter(serialDevice, blobDevice, eventDevice);

        final RPCDevice device = new ObjectDevice(new BlobSource(), "blobs");
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

    // ------------------------------------------------------------- //

    @Test
    public void payloadGoesOutOnTheBlobPortAndIsDescribedByTheMessage() {
        final JsonObject reply = invoke("readBlob");

        final JsonObject blob = reply.getAsJsonObject("blob");
        assertNotNull(blob, "the message must describe the payload: " + reply);
        assertEquals(PAYLOAD.length, blob.get("length").getAsInt());
        assertEquals(RPCPayloadChannel.checksum(PAYLOAD), blob.get("checksum").getAsInt());

        assertTrue(reply.getAsJsonObject("data").get("$blob").getAsBoolean(),
                "the data should carry only a marker");
        assertArrayEquals(PAYLOAD, blobDevice.drainAsVM(), "the payload goes out on the blob port");
    }

    @Test
    public void theReplyStaysSmallHoweverBigThePayloadIs() {
        final JsonObject large = invoke("readLarge");

        assertTrue(large.toString().length() < 200,
                "message size must not track payload size: " + large);
        assertEquals(64 * 1024, blobDevice.drainAsVM().length);
    }

    @Test
    public void everyByteValueSurvives() {
        invoke("readAllByteValues");

        final byte[] sent = blobDevice.drainAsVM();
        assertEquals(256, sent.length);
        for (int i = 0; i < 256; i++) {
            assertEquals((byte) i, sent[i], "byte value " + i + " did not survive");
        }
    }

    @Test
    public void deviceDataCannotForgeAPayloadReference() {
        final JsonObject reply = invoke("readForgedReference");

        assertFalse(reply.has("blob"),
                "no payload was sent, so the message must not announce one: " + reply);
        assertEquals(0, blobDevice.drainAsVM().length);
    }

    @Test
    public void twoPayloadsInOneMessageAreRefusedAndDoNotLeak() {
        final JsonObject reply = invoke("readTwoBlobs");
        assertEquals("error", reply.get("type").getAsString(), reply.toString());
        assertEquals(0, blobDevice.drainAsVM().length, "a refused message must send no bytes");

        // The staged payload must not leak into the next message.
        final JsonObject next = invoke("readBlob");
        assertEquals(PAYLOAD.length, next.getAsJsonObject("blob").get("length").getAsInt());
        assertArrayEquals(PAYLOAD, blobDevice.drainAsVM());
    }

    @Test
    public void failingCallSendsNoPayload() {
        final JsonObject reply = invoke("readThenFail");

        assertEquals("error", reply.get("type").getAsString(), reply.toString());
        assertEquals(0, blobDevice.drainAsVM().length,
                "a call that threw after staging must not put bytes on the wire");
    }

    @Test
    public void noNewRequestIsTakenWhileAPayloadIsStillDraining() {
        setUpWith(new TestSerialDevice(), new TestSerialDevice(1024));

        invoke("readLarge");
        assertTrue(blobDevice.drainAsVM().length < 64 * 1024, "precondition: drain is partial");

        serialDevice.putAsVM(request("readBlob"));
        adapter.step(0);
        assertNull(serialDevice.readMessageAsVM(), "a request was answered mid-transfer");

        for (int i = 0; i < 200; i++) {
            adapter.step(0);
            blobDevice.drainAsVM();
        }
        assertNotNull(serialDevice.readMessageAsVM(), "request was never answered after the drain");
    }

    @Test
    public void resetDropsAPartiallyDrainedPayload() {
        setUpWith(new TestSerialDevice(), new TestSerialDevice(1024));

        invoke("readLarge");
        blobDevice.drainAsVM();

        adapter.reset();
        adapter.step(0);
        assertEquals(0, blobDevice.drainAsVM().length, "reset must abandon the transfer");

        assertEquals(PAYLOAD.length, invoke("readBlob").getAsJsonObject("blob").get("length").getAsInt());
    }

    @Test
    public void checksumMatchesTheAgreedAlgorithm() {
        assertEquals(746590911, RPCPayloadChannel.checksum(PAYLOAD));
        assertEquals(0, RPCPayloadChannel.checksum(new byte[0]));
        assertEquals(513, RPCPayloadChannel.checksum(new byte[]{1, 2}));
        assertEquals(258, RPCPayloadChannel.checksum(new byte[]{2, 1}));
    }

    @Test
    public void checksumChangesWhenAnyByteDoes() {
        final byte[] corrupted = PAYLOAD.clone();
        corrupted[3] ^= 0x01;

        assertNotEquals(RPCPayloadChannel.checksum(PAYLOAD),
                RPCPayloadChannel.checksum(corrupted),
                "a single flipped bit must change the checksum, or it detects nothing");
        assertNotEquals(RPCPayloadChannel.checksum(new byte[]{1, 2}),
                RPCPayloadChannel.checksum(new byte[]{2, 1}),
                "reordering must change the checksum too");
    }

    @Test
    public void emptyPayloadStillRoundTrips() {
        final JsonObject reply = invoke("readEmpty");

        assertEquals(0, reply.getAsJsonObject("blob").get("length").getAsInt());
        assertEquals(0, blobDevice.drainAsVM().length, "an empty payload should send no bytes");
    }

    @Test
    public void payloadPastTheCapIsRefusedBeforeAnyOfItIsSent() {
        final JsonObject reply = invoke("readPastTheCap");

        assertEquals("error", reply.get("type").getAsString(), reply.toString());
        assertEquals(RPCDeviceBusAdapter.ERROR_PAYLOAD_TOO_LARGE, reply.get("data").getAsString());
        assertEquals(0, blobDevice.drainAsVM().length, "part of an over-large payload was sent");

        assertEquals(PAYLOAD.length, invoke("readBlob").getAsJsonObject("blob").get("length").getAsInt());
        assertArrayEquals(PAYLOAD, blobDevice.drainAsVM());
    }

    // ------------------------------------------------------------- //

    private String request(final String method) {
        return "{\"type\":\"invoke\",\"data\":{\"deviceId\":\"" + deviceId
                + "\",\"name\":\"" + method + "\",\"parameters\":[]}}";
    }

    private JsonObject invoke(final String method) {
        serialDevice.putAsVM(request(method));
        adapter.step(0);

        final String message = serialDevice.readMessageAsVM();
        assertNotNull(message, "no reply");
        return JsonParser.parseString(message).getAsJsonObject();
    }

    public static final class BlobSource {
        @Callback(synchronize = false)
        public byte[] readPastTheCap() {
            return new byte[Constants.RPC_MAX_PAYLOAD_SIZE + 1];
        }

        @Callback(synchronize = false)
        public byte[] readBlob() {
            return PAYLOAD;
        }

        @Callback(synchronize = false)
        public byte[] readEmpty() {
            return new byte[0];
        }

        @Callback(synchronize = false)
        public byte[] readLarge() {
            return new byte[64 * 1024];
        }

        @Callback(synchronize = false)
        public byte[] readAllByteValues() {
            final byte[] data = new byte[256];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) i;
            }
            return data;
        }

        @Callback(synchronize = false)
        public Map<String, Object> readForgedReference() {
            return Map.of("$blob", Map.of("length", 100000, "checksum", 0));
        }

        @Callback(synchronize = false)
        public Map<String, byte[]> readTwoBlobs() {
            return Map.of("a", new byte[]{1}, "b", new byte[]{2});
        }

        @Callback(synchronize = false)
        public Object readThenFail() {
            throw new IllegalStateException("deliberate");
        }
    }
}
