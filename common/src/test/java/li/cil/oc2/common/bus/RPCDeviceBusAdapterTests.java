/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.TypeNameDevice;
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
        adapter = new RPCDeviceBusAdapter(mock(SerialDevice.class), mock(SerialDevice.class), mock(SerialDevice.class), amount -> true);
        busDevices = new HashSet<>();
        deviceIdentifiers = new HashMap<>();
        controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(busDevices);
        when(controller.getDeviceIdentifiers(any())).then(invocation -> deviceIdentifiers.get((Device) invocation.getArgument(0)));
    }

    // --------------------------------------------------------------------- //

    @Test
    public void unattributableReplyEchoesNoRequestId() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
            serial, new TestSerialDevice(), new TestSerialDevice(), amount -> true);
        addDevice();
        busAdapter.rebuild(controller);

        serial.putAsVM("{\"type\":\"list\",\"id\":11}");
        busAdapter.step(0);
        assertEquals(11, serial.readJsonAsVM().get("id").getAsInt());

        serial.putAsVM("not json at all");
        busAdapter.step(0);

        final JsonObject refusal = serial.readJsonAsVM();
        assertEquals("error", refusal.get("type").getAsString());
        assertEquals(0, refusal.get("id").getAsInt(), "a stale id leaked onto an unattributable reply");
    }

    @Test
    public void guestThatSendsNoIdIsStillAnswered() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
            serial, new TestSerialDevice(), new TestSerialDevice(), amount -> true);
        addDevice();
        busAdapter.rebuild(controller);

        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);

        final JsonObject reply = serial.readJsonAsVM();
        assertEquals("list", reply.get("type").getAsString());
        assertEquals(0, reply.get("id").getAsInt());
    }

    @Test
    public void idThatIsNotANumberIsRefused() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
            serial, new TestSerialDevice(), new TestSerialDevice(), amount -> true);
        addDevice();
        busAdapter.rebuild(controller);

        serial.putAsVM("{\"type\":\"list\",\"id\":\"not a number\"}");
        busAdapter.step(0);

        final JsonObject refusal = serial.readJsonAsVM();
        assertEquals("error", refusal.get("type").getAsString());
        assertEquals(0, refusal.get("id").getAsInt(), "an unparseable request names no id");
    }

    @Test
    public void oversizedMessageIsRefusedAndTheChannelRecovers() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
            serial, new TestSerialDevice(), new TestSerialDevice(), amount -> true);
        addDevice();
        busAdapter.rebuild(controller);

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
    public void deviceOnTwoElementsIsExposedOnceUnderTheLowerIdentifier() {
        final UUID first = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        final UUID second = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
            serial, new TestSerialDevice(), new TestSerialDevice(), amount -> true);
        addDevice(new ObjectDevice(new Pingable(), "pingable"), second, first);
        busAdapter.rebuild(controller);

        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);

        final JsonArray listed = serial.readJsonAsVM().getAsJsonArray("data");
        assertEquals(1, listed.size(), "the same device was exposed twice");

        final UUID chosen = first.compareTo(second) <= 0 ? first : second;
        assertEquals(chosen.toString(), listed.get(0).getAsJsonObject().get("deviceId").getAsString());

        busAdapter.rebuild(controller);
        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);
        assertEquals(chosen.toString(), serial.readJsonAsVM().getAsJsonArray("data")
                .get(0).getAsJsonObject().get("deviceId").getAsString(),
            "the exposed identifier changed across a rebuild");
    }

    @Test
    public void resumeDoesNotMountDirectly() {
        final RPCDevice device1 = addDevice();

        adapter.rebuild(controller);
        verify(device1, never()).mount(any());
    }

    @Test
    public void emptyDevicesAreNotMounted() {
        final RPCDevice device = addEmptyDevice();
        adapter.rebuild(controller);

        adapter.mountDevices();
        verify(device, never()).mount(any());
    }

    @Test
    public void addedDevicesHaveMountCalled() {
        final RPCDevice device = addDevice();
        adapter.rebuild(controller);

        adapter.mountDevices();
        verify(device).mount(any());
    }

    @Test
    public void mountedDevicesAreUnmountedWhenRemoved() {
        final RPCDevice device = addDevice();
        adapter.rebuild(controller);
        adapter.mountDevices();

        removeDevice(device);
        adapter.rebuild(controller);
        verify(device).unmount(any());
        verify(device, never()).dispose();
    }

    @Test
    public void unmountedDevicesAreSilentlyRemoved() {
        final RPCDevice device = addDevice();
        adapter.rebuild(controller);

        removeDevice(device);
        adapter.rebuild(controller);
        verify(device, never()).unmount(any());
        verify(device, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedButNotDisposedOnGlobalUnmount() {
        final RPCDevice device = addDevice();
        adapter.rebuild(controller);
        adapter.mountDevices();

        adapter.unmountDevices();
        verify(device).unmount(any());
        verify(device, never()).dispose();
    }

    @Test
    public void globalUnmountSkipsUnmountedDevices() {
        final RPCDevice device = addDevice();
        adapter.rebuild(controller);

        adapter.unmountDevices();
        verify(device, never()).unmount(any());
        verify(device, never()).dispose();
    }

    @Test
    public void devicesHaveMountCalledAfterGlobalUnmount() {
        final RPCDevice device = addDevice();
        adapter.rebuild(controller);
        adapter.mountDevices();
        adapter.unmountDevices();

        adapter.mountDevices();
        verify(device, times(2)).mount(any());
    }

    @Test
    public void deviceListIsStable() {
        final RPCDevice device1 = mockDevice();
        final RPCDevice device2 = mockDevice();
        final UUID identifier = UUID.randomUUID();
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        when(device2.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(device1, identifier);
        addDevice(device2, identifier);

        adapter.rebuild(controller);
        verify(device1, never()).mount(any());
        verify(device2, never()).mount(any());

        adapter.mountDevices();
        verify(device1).mount(any());
        verify(device2).mount(any());

        adapter.rebuild(controller);

        verify(device1, never()).unmount(any());
        verify(device2, never()).unmount(any());

        adapter.mountDevices();
        verify(device1, atMostOnce()).mount(any());
        verify(device2, atMostOnce()).mount(any());
    }

    @Test
    public void aliasesAreListedAsTypeNames() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
            serial, new TestSerialDevice(), new TestSerialDevice(), amount -> true);
        final UUID identifier = UUID.randomUUID();
        final RPCDevice device = addDevice();
        when(device.getTypeName()).thenReturn("redstone");
        deviceIdentifiers.put(device, Set.of(identifier));
        addDevice(new TypeNameDevice("test_device"), identifier);
        addDevice(new TypeNameDevice("oc2:redstone_interface"), identifier);
        busAdapter.rebuild(controller);

        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);

        final JsonArray devices = serial.readJsonAsVM().getAsJsonArray("data");
        assertEquals(1, devices.size());
        assertEquals(JsonParser.parseString("[\"oc2:redstone_interface\",\"redstone\",\"test_device\"]"),
            devices.get(0).getAsJsonObject().get("typeNames"));
    }

    @Test
    public void aliasesWithoutDevicesAreHidden() {
        final TestSerialDevice serial = new TestSerialDevice();
        final RPCDeviceBusAdapter busAdapter = new RPCDeviceBusAdapter(
            serial, new TestSerialDevice(), new TestSerialDevice(), amount -> true);
        addDevice(new TypeNameDevice("test_device"));
        busAdapter.rebuild(controller);

        serial.putAsVM("{\"type\":\"list\"}");
        busAdapter.step(0);

        assertEquals(0, serial.readJsonAsVM().getAsJsonArray("data").size());
    }

    // --------------------------------------------------------------------- //

    private static RPCDevice mockDevice() {
        final RPCDevice device = mock(RPCDevice.class);
        when(device.getTypeName()).thenReturn("test");
        return device;
    }

    private RPCDevice addEmptyDevice() {
        final RPCDevice device = mockDevice();
        addDevice(device);
        return device;
    }

    private RPCDevice addDevice() {
        final RPCDevice device = mockDevice();
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
}
