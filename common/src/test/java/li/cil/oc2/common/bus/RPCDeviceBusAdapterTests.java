/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceList;
import li.cil.sedna.api.device.serial.SerialDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public final class RPCDeviceBusAdapterTests {
    private RPCDeviceBusAdapter adapter;
    private Set<Device> busDevices;
    private Map<Device, Set<UUID>> deviceIdentifiers;
    private DeviceBusController controller;

    @BeforeEach
    public void setupEach() {
        adapter = new RPCDeviceBusAdapter(mock(SerialDevice.class), mock(SerialDevice.class), mock(SerialDevice.class));
        busDevices = new HashSet<>();
        deviceIdentifiers = new HashMap<>();
        controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(busDevices);
        when(controller.getDeviceIdentifiers(any())).then(invocation -> deviceIdentifiers.get((Device) invocation.getArgument(0)));
    }

    @Test
    public void theRequestIdComesBackOnTheReply() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
                serial, new TestSerialDevice(), new TestSerialDevice());
        addDevice();
        busAdapter.resume(controller, true);

        serial.putAsVM("{\"type\":\"list\",\"id\":4711}");
        busAdapter.step(0);

        final JsonObject reply = JsonParser.parseString(serial.readMessageAsVM()).getAsJsonObject();
        assertEquals("list", reply.get("type").getAsString());
        assertEquals(4711, reply.get("id").getAsInt(),
                "without the id back the guest cannot tell whose answer this is");
    }

    @Test
    public void aReplyTheHostCannotAttributeNamesNoRequest() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
                serial, new TestSerialDevice(), new TestSerialDevice());
        addDevice();
        busAdapter.resume(controller, true);

        serial.putAsVM("{\"type\":\"list\",\"id\":11}");
        busAdapter.step(0);
        assertEquals(11, JsonParser.parseString(serial.readMessageAsVM())
                .getAsJsonObject().get("id").getAsInt());

        serial.putAsVM("not json at all");
        busAdapter.step(0);

        final JsonObject refusal = JsonParser.parseString(serial.readMessageAsVM()).getAsJsonObject();
        assertEquals("error", refusal.get("type").getAsString());
        assertEquals(0, refusal.get("id").getAsInt(), "a stale id leaked onto an unattributable reply");
    }

    @Test
    public void aGuestThatSendsNoIdIsStillAnswered() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
                serial, new TestSerialDevice(), new TestSerialDevice());
        addDevice();
        busAdapter.resume(controller, true);

        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);

        final JsonObject reply = JsonParser.parseString(serial.readMessageAsVM()).getAsJsonObject();
        assertEquals("list", reply.get("type").getAsString());
        assertEquals(0, reply.get("id").getAsInt());
    }

    @Test
    public void anIdThatIsNotANumberIsRefused() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
                serial, new TestSerialDevice(), new TestSerialDevice());
        addDevice();
        busAdapter.resume(controller, true);

        serial.putAsVM("{\"type\":\"list\",\"id\":\"not a number\"}");
        busAdapter.step(0);

        final JsonObject refusal = JsonParser.parseString(serial.readMessageAsVM()).getAsJsonObject();
        assertEquals("error", refusal.get("type").getAsString());
        assertEquals(0, refusal.get("id").getAsInt(), "an unparseable request names no id");
    }

    @Test
    public void anOversizedMessageIsRefusedAndTheChannelRecovers() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
                serial, new TestSerialDevice(), new TestSerialDevice());
        addDevice();
        busAdapter.resume(controller, true);

        final StringBuilder tooLong = new StringBuilder("{\"type\":\"list\",\"pad\":\"");
        while (tooLong.length() < 8 * Constants.KILOBYTE) {
            tooLong.append('x');
        }
        tooLong.append("\"}");
        serial.putAsVM(tooLong.toString());
        busAdapter.step(0);

        final String refusal = serial.readMessageAsVM();
        assertNotNull(refusal, "an over-long message got no reply, so a guest would wait forever");
        assertEquals(RPCDeviceBusAdapter.ERROR_MESSAGE_TOO_LARGE,
                JsonParser.parseString(refusal).getAsJsonObject().get("data").getAsString());

        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);
        final String next = serial.readMessageAsVM();
        assertNotNull(next, "the channel never recovered");
        assertEquals("list", JsonParser.parseString(next).getAsJsonObject().get("type").getAsString());
    }

    @Test
    public void aDeviceOnTwoElementsIsExposedOnceUnderTheLowerIdentifier() {
        final UUID first = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        final UUID second = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
                serial, new TestSerialDevice(), new TestSerialDevice());
        addDevice(new ObjectDevice(new Pingable(), "pingable"), second, first);
        busAdapter.resume(controller, true);

        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);

        final JsonArray listed = JsonParser.parseString(serial.readMessageAsVM())
                .getAsJsonObject().getAsJsonArray("data");
        assertEquals(1, listed.size(), "the same device was exposed twice");

        final UUID chosen = first.compareTo(second) <= 0 ? first : second;
        assertEquals(chosen.toString(),
                listed.get(0).getAsJsonObject().get("deviceId").getAsString());

        busAdapter.resume(controller, true);
        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);
        assertEquals(chosen.toString(), JsonParser.parseString(serial.readMessageAsVM())
                        .getAsJsonObject().getAsJsonArray("data")
                        .get(0).getAsJsonObject().get("deviceId").getAsString(),
                "the exposed identifier changed across a rebuild");
    }

    @Test
    public void resumeDoesNotMountDirectly() {
        final RPCDevice device1 = addDevice();

        adapter.resume(controller, true);
        verify(device1, never()).mount();
    }

    @Test
    public void emptyDevicesAreNotMounted() {
        final RPCDevice device = addEmptyDevice();
        adapter.resume(controller, true);

        adapter.mountDevices();
        verify(device, never()).mount();
    }

    @Test
    public void addedDevicesHaveMountCalled() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);

        adapter.mountDevices();
        verify(device).mount();
    }

    @Test
    public void mountedDevicesAreUnmountedWhenRemoved() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);
        adapter.mountDevices();

        removeDevice(device);
        adapter.resume(controller, true);
        verify(device).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void unmountedDevicesAreSilentlyRemoved() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);

        removeDevice(device);
        adapter.resume(controller, true);
        verify(device, never()).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedButNotDisposedOnGlobalUnmount() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);
        adapter.mountDevices();

        adapter.unmountDevices();
        verify(device).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void unmountedDevicesAreNotUnmountedAndNotDisposedOnGlobalUnmount() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);

        adapter.unmountDevices();
        verify(device, never()).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedAndDisposedOnGlobalDispose() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);
        adapter.mountDevices();

        adapter.disposeDevices();
        verify(device).unmount();
        verify(device).dispose();
    }

    @Test
    public void unmountedDevicesAreNotUnmountedButDisposedOnGlobalDispose() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);

        adapter.disposeDevices();
        verify(device, never()).unmount();
        verify(device).dispose();
    }

    @Test
    public void devicesHaveMountCalledAfterGlobalUnmount() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);
        adapter.mountDevices();
        adapter.unmountDevices();

        adapter.mountDevices();
        verify(device, times(2)).mount();
    }

    @Test
    public void deviceListIsStable() {
        final RPCDevice device1 = mock(RPCDevice.class);
        final RPCDevice device2 = mock(RPCDevice.class);
        final RPCDevice listDevice = new RPCDeviceList(new ArrayList<>(Arrays.asList(device1, device2)));
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        when(device2.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(listDevice);

        adapter.resume(controller, true);
        verify(device1, never()).mount();
        verify(device2, never()).mount();

        adapter.mountDevices();
        verify(device1).mount();
        verify(device2).mount();

        adapter.resume(controller, true);

        verify(device1, never()).unmount();
        verify(device2, never()).unmount();

        adapter.mountDevices();
        verify(device1, atMostOnce()).mount();
        verify(device2, atMostOnce()).mount();
    }

    private RPCDevice addEmptyDevice() {
        final RPCDevice device = mock(RPCDevice.class);
        addDevice(device);
        return device;
    }

    private RPCDevice addDevice() {
        final RPCDevice device = mock(RPCDevice.class);
        when(device.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(device);
        return device;
    }

    private void addDevice(final Device device, UUID... identifiers) {
        if (identifiers.length == 0) {
            identifiers = new UUID[]{UUID.randomUUID()};
        }

        busDevices.add(device);
        deviceIdentifiers.put(device, new HashSet<>(Arrays.asList(identifiers)));
    }

    private void removeDevice(final Device device) {
        busDevices.remove(device);
        deviceIdentifiers.remove(device);
    }

    public static final class Pingable {
        @Callback(synchronize = false)
        public int ping() {
            return 1;
        }
    }
}
