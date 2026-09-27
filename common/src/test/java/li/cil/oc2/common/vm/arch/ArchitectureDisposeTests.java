/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.arch;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IOBusContext;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.rpc.RPCBusContext;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.vm.DeviceLocation;
import li.cil.oc2.common.vm.provider.DeviceDescriptionProviders;
import li.cil.sedna.Sedna;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

public final class ArchitectureDisposeTests {
    private static final UUID DEVICE_UUID = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    private DeviceBusController controller;

    @BeforeAll
    public static void setup() {
        Sedna.initialize();
        DeviceDescriptionProviders.initialize();
    }

    @BeforeEach
    public void setupEach() {
        controller = mock(DeviceBusController.class);
    }

    @Test
    public void deviceOfSeveralAdaptersIsDisposedOnceAfterAllUnmounts() {
        final VMDevice device = mock(VMDevice.class, withSettings().extraInterfaces(RPCDevice.class));
        final RPCDevice rpcDevice = (RPCDevice) device;
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());
        when(rpcDevice.getMethodGroups()).thenReturn(List.of(mock(RPCMethod.class)));
        setDevices(device);

        final R5Architecture architecture = new R5Architecture(config());
        start(architecture);

        architecture.stopAndReset();

        final InOrder order = inOrder(device);
        order.verify(device).unmount();
        order.verify(rpcDevice).unmount(any(RPCBusContext.class));
        order.verify(device).dispose();
        verify(device, times(1)).dispose();
    }

    @Test
    public void deviceWithoutAdapterIsDisposed() {
        final Device device = mock(Device.class);
        setDevices(device);

        final R5Architecture architecture = new R5Architecture(config());
        start(architecture);

        architecture.stopAndReset();

        verify(device).dispose();
    }

    @Test
    public void midLevelApiDeviceIsUnmountedThenDisposedOnZ80() {
        final IODevice device = mock(IODevice.class, withSettings().extraInterfaces(Device.class));
        when(device.getIOName()).thenReturn("TEST");
        when(device.getIOMethods()).thenReturn(List.of(mock(IOMethod.class)));
        setDevices((Device) device);

        final Z80Architecture architecture = new Z80Architecture(config());
        start(architecture);

        architecture.stopAndReset();

        final InOrder order = inOrder(device);
        order.verify(device).mountIO(any(IOBusContext.class));
        order.verify(device).unmountIO(any(IOBusContext.class));
        order.verify((Device) device).dispose();
    }

    // --------------------------------------------------------------------- //

    private AbstractArchitecture.Config config() {
        return new AbstractArchitecture.Config(unused -> DeviceLocation.UNSPECIFIED, () -> {
        }, () -> 0L);
    }

    private void setDevices(final Device... devices) {
        when(controller.getDevices()).thenReturn(Set.of(devices));
        for (final Device device : devices) {
            when(controller.getDeviceIdentifiers(device)).thenReturn(Set.of(DEVICE_UUID));
        }
    }

    private void start(final AbstractArchitecture architecture) {
        architecture.handleAfterDeviceScan(controller);
        architecture.mountVMDevices();
        architecture.mountDynamicDevices();
    }
}
