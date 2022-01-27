package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceList;
import li.cil.sedna.api.device.serial.SerialDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.mockito.Mockito.*;

public final class RPCDeviceTests {
    private RPCDeviceBusAdapter adapter;
    private Set<Device> busDevices;
    private Map<Device, Set<UUID>> deviceIdentifiers;
    private DeviceBusController controller;

    @BeforeEach
    public void setupEach() {
        adapter = new RPCDeviceBusAdapter(mock(SerialDevice.class));
        busDevices = new HashSet<>();
        deviceIdentifiers = new HashMap<>();
        controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(busDevices);
        when(controller.getDeviceIdentifiers(any())).then(invocation -> deviceIdentifiers.get((Device) invocation.getArgument(0)));
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
    public void mountedDevicesAreUnmountedAndDisposedWhenRemoved() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);
        adapter.mountDevices();

        removeDevice(device);
        adapter.resume(controller, true);
        verify(device).unmount();
        verify(device).dispose();
    }

    @Test
    public void unmountedDevicesAreDisposedWhenRemoved() {
        final RPCDevice device = addDevice();
        adapter.resume(controller, true);

        removeDevice(device);
        adapter.resume(controller, true);
        verify(device, never()).unmount();
        verify(device).dispose();
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
}
