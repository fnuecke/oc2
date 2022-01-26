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
    public void emptyDevicesAreNotMounted() {
        final RPCDevice device1 = mock(RPCDevice.class);
        addDevice(device1);

        adapter.resume(controller, true);
        adapter.mount();
        verify(device1, never()).mount();
    }

    @Test
    public void addedDevicesHaveMountCalled() {
        final RPCDevice device1 = mock(RPCDevice.class);
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(device1);

        adapter.resume(controller, true);
        verify(device1, never()).mount();

        adapter.mount();
        verify(device1).mount();
    }

    @Test
    public void mountedDevicesAreUnmountedWhenRemoved() {
        final RPCDevice device1 = mock(RPCDevice.class);
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(device1);

        adapter.resume(controller, true);
        verify(device1, never()).mount();

        adapter.mount();
        verify(device1).mount();

        removeDevice(device1);
        adapter.resume(controller, true);
        verify(device1).unmount();
    }

    @Test
    public void mountedDevicesAreUnmountedOnGlobalUnmount() {
        final RPCDevice device1 = mock(RPCDevice.class);
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(device1);

        adapter.resume(controller, true);
        verify(device1, never()).mount();

        adapter.mount();
        verify(device1).mount();

        adapter.unmount();
        verify(device1).unmount();
    }

    @Test
    public void unmountedDevicesAreNotUnmountedOnGlobalUnmount() {
        final RPCDevice device1 = mock(RPCDevice.class);
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(device1);

        adapter.resume(controller, true);
        verify(device1, never()).mount();

        adapter.unmount();
        verify(device1, never()).unmount();
    }

    @Test
    public void deviceListForwardsMount() {
        final RPCDevice device1 = mock(RPCDevice.class);
        final RPCDevice device2 = mock(RPCDevice.class);
        final RPCDevice listDevice = new RPCDeviceList(new ArrayList<>(Arrays.asList(device1, device2)));
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        when(device2.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(listDevice);

        adapter.resume(controller, true);
        verify(device1, never()).mount();
        verify(device2, never()).mount();

        adapter.mount();
        verify(device1).mount();
        verify(device2).mount();
    }

    @Test
    public void deviceListForwardsUnmount() {
        final RPCDevice device1 = mock(RPCDevice.class);
        final RPCDevice device2 = mock(RPCDevice.class);
        final RPCDevice listDevice = new RPCDeviceList(new ArrayList<>(Arrays.asList(device1, device2)));
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        when(device2.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(listDevice);

        adapter.resume(controller, true);
        verify(device1, never()).mount();
        verify(device2, never()).mount();

        adapter.mount();
        verify(device1).mount();
        verify(device2).mount();

        adapter.unmount();
        verify(device1).unmount();
        verify(device2).unmount();
    }

    @Test
    public void deviceListForwardsSuspend() {
        final RPCDevice device1 = mock(RPCDevice.class);
        final RPCDevice device2 = mock(RPCDevice.class);
        final RPCDevice listDevice = new RPCDeviceList(new ArrayList<>(Arrays.asList(device1, device2)));
        when(device1.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        when(device2.getMethodGroups()).thenReturn(Collections.singletonList(mock(RPCMethod.class)));
        addDevice(listDevice);

        adapter.resume(controller, true);
        verify(device1, never()).mount();
        verify(device2, never()).mount();

        adapter.mount();
        verify(device1).mount();
        verify(device2).mount();

        adapter.suspend();
        verify(device1).suspend();
        verify(device2).suspend();
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

        adapter.mount();
        verify(device1).mount();
        verify(device2).mount();

        adapter.resume(controller, true);

        verify(device1, never()).unmount();
        verify(device2, never()).unmount();

        adapter.mount();
        verify(device1, atMostOnce()).mount();
        verify(device2, atMostOnce()).mount();
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
