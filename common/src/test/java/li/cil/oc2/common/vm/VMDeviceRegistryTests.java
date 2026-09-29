/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.api.Board;
import li.cil.sedna.api.DeviceBus;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.device.rtc.RealTimeCounter;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.api.memory.MemoryRangeAllocationStrategy;
import li.cil.sedna.memory.SimpleMemoryMap;
import li.cil.sedna.riscv.R5MemoryRangeAllocationStrategy;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public final class VMDeviceRegistryTests {
    private MemoryMap memoryMap;
    private InterruptController interruptController;
    private R5MemoryRangeAllocationStrategy allocationStrategy;
    private GlobalVMContext context;
    private VMDeviceRegistry registry;
    private DeviceBusController controller;
    private final Set<Device> busDevices = new HashSet<>();

    @BeforeEach
    public void setupEach() {
        memoryMap = new SimpleMemoryMap();
        interruptController = new R5PlatformLevelInterruptController();
        allocationStrategy = new R5MemoryRangeAllocationStrategy();

        final DeviceBus deviceBus = mock(DeviceBus.class);
        when(deviceBus.getMemoryMap()).thenReturn(memoryMap);
        when(deviceBus.addDevice(any())).then(invocation -> {
            final MemoryMappedDevice device = invocation.getArgument(0);
            final OptionalLong address = allocationStrategy.findMemoryRange(device, MemoryRangeAllocationStrategy.getMemoryMapIntersectionProvider(memoryMap));
            if (address.isPresent() && memoryMap.addDevice(address.getAsLong(), device)) {
                return address;
            }
            return OptionalLong.empty();
        });
        doAnswer(invocation -> {
            memoryMap.removeDevice(invocation.getArgument(0));
            return null;
        }).when(deviceBus).removeDevice(any());

        final Board board = mock(Board.class);
        when(board.getMemoryMap()).thenReturn(memoryMap);
        when(board.getDeviceBus()).thenReturn(deviceBus);
        when(board.getInterruptController()).thenReturn(interruptController);
        when(board.getInterruptCount()).thenReturn(16);

        context = new GlobalVMContext(board, mock(RealTimeCounter.class), () -> {
        }, null);
        registry = new VMDeviceRegistry(context, unused -> OptionalLong.empty());
        controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(busDevices);
    }

    @Test
    public void addingDeviceDoesNotMountDirectly() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(device);
        verify(device, never()).mount(any());
    }

    @Test
    public void addedDevicesHaveMountCalled() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());
        verify(device).mount(any());
    }

    @Test
    public void existingDevicesDoNotHaveMountCalledAgain() {
        final VMDevice device1 = mock(VMDevice.class);
        final VMDevice device2 = mock(VMDevice.class);
        when(device1.mount(any())).thenReturn(VMDeviceLoadResult.success());
        when(device2.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(device1);
        registry.mountDevices();
        verify(device1).mount(any());

        addDevice(device2);
        registry.mountDevices();
        verifyNoMoreInteractions(device1);
    }

    @Test
    public void deviceFailingMountDoesNotHaveUnmountOrDisposeCalled() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.fail());

        addDevice(device);
        assertFalse(registry.mountDevices().wasSuccessful());
        verify(device).mount(any());
        verify(device, never()).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedWhenRemoved() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());

        removeDevice(device);
        verify(device).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void unmountedDevicesAreSilentlyRemoved() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(device);
        registry.mountDevices();
        verify(device).mount(any());

        registry.unmountDevices();
        verify(device).unmount();

        removeDevice(device);

        verify(device, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedButNotDisposedOnGlobalUnmount() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());

        registry.unmountDevices();
        verify(device).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void globalUnmountSkipsUnmountedDevices() {
        final VMDevice device = mock(VMDevice.class);

        addDevice(device);

        registry.unmountDevices();
        verify(device, never()).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void devicesHaveMountCalledAfterGlobalUnmount() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(device);
        registry.mountDevices();
        registry.unmountDevices();

        assertTrue(registry.mountDevices().wasSuccessful());
        verify(device, times(2)).mount(any());
    }

    @Test
    public void deviceCanClaimInterrupts() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);
            final OptionalInt interrupt = context.getInterruptAllocator().claimInterrupt();
            assertTrue(interrupt.isPresent());
            return VMDeviceLoadResult.success();
        });

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());

        verify(device).mount(any());
    }

    @Test
    public void deviceCannotClaimClaimedInterrupts() {
        final int claimedInterrupt = 1;

        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);
            final boolean result = context.getInterruptAllocator().claimInterrupt(claimedInterrupt);
            assertFalse(result);
            return VMDeviceLoadResult.success();
        });

        context.getInterruptAllocator().claimInterrupt(claimedInterrupt);

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());
    }

    @Test
    public void deviceCanRaiseClaimedInterrupts() {
        final DeviceData deviceData = new DeviceData();
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);
            final OptionalInt interrupt = context.getInterruptAllocator().claimInterrupt();
            assertTrue(interrupt.isPresent());

            deviceData.context = context;
            deviceData.interrupt = interrupt.getAsInt();

            return VMDeviceLoadResult.success();
        });

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());

        final int claimedInterruptMask = 1 << deviceData.interrupt;
        deviceData.context.getInterruptController().raiseInterrupts(claimedInterruptMask);

        assertTrue((interruptController.getRaisedInterrupts() & claimedInterruptMask) != 0);
    }

    @Test
    public void devicesCannotRaiseUnclaimedInterrupts() {
        final DeviceData deviceData = new DeviceData();
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenAnswer(invocation -> {
            deviceData.context = invocation.getArgument(0);
            return VMDeviceLoadResult.success();
        });

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());

        final int someInterruptMask = 0x1;
        assertThrows(IllegalArgumentException.class, () ->
            deviceData.context.getInterruptController().raiseInterrupts(someInterruptMask));
    }

    @Test
    public void unmountLowersClaimedInterrupts() {
        final DeviceData deviceData = new DeviceData();
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);
            final OptionalInt interrupt = context.getInterruptAllocator().claimInterrupt();
            assertTrue(interrupt.isPresent());

            deviceData.context = context;
            deviceData.interrupt = interrupt.getAsInt();

            return VMDeviceLoadResult.success();
        });

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());

        final int claimedInterruptMask = 1 << deviceData.interrupt;
        deviceData.context.getInterruptController().raiseInterrupts(claimedInterruptMask);

        assertTrue((interruptController.getRaisedInterrupts() & claimedInterruptMask) != 0);

        registry.unmountDevices();

        assertFalse((interruptController.getRaisedInterrupts() & claimedInterruptMask) != 0);
    }

    @Test
    public void devicesCannotAddToMemoryMapDirectly() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);

            assertThrows(UnsupportedOperationException.class, () ->
                context.getMemoryMap().addDevice(0, mock(MemoryMappedDevice.class)));

            return VMDeviceLoadResult.success();
        });

        addDevice(device);
        registry.mountDevices();
    }

    @Test
    public void devicesCanAddMemoryMappedDevices() {
        final DeviceData deviceData = new DeviceData();
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);

            deviceData.context = context;
            deviceData.device = mock(MemoryMappedDevice.class);
            when(deviceData.device.getLength()).thenReturn(0x1000);

            assertTrue(context.getMemoryRangeAllocator().claimMemoryRange(deviceData.device).isPresent());

            return VMDeviceLoadResult.success();
        });

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());

        assertTrue(deviceData.context.getMemoryMap().getMemoryRange(deviceData.device).isPresent());
    }

    @Test
    public void addedDevicesGetRemovedOnUnmount() {
        final DeviceData deviceData = new DeviceData();
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);

            deviceData.context = context;
            deviceData.device = mock(MemoryMappedDevice.class);
            when(deviceData.device.getLength()).thenReturn(0x1000);

            assertTrue(context.getMemoryRangeAllocator().claimMemoryRange(deviceData.device).isPresent());

            return VMDeviceLoadResult.success();
        });

        addDevice(device);
        assertTrue(registry.mountDevices().wasSuccessful());

        assertTrue(deviceData.context.getMemoryMap().getMemoryRange(deviceData.device).isPresent());

        registry.unmountDevices();

        assertFalse(deviceData.context.getMemoryMap().getMemoryRange(deviceData.device).isPresent());
    }

    @Test
    public void failedMountLeavesAlreadyMountedDevicesRecoverable() {
        final VMDevice mounted = mock(VMDevice.class);
        when(mounted.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(mounted);
        assertTrue(registry.mountDevices().wasSuccessful());
        verify(mounted, times(1)).mount(any());

        final VMDevice failing = mock(VMDevice.class);
        when(failing.mount(any())).thenReturn(VMDeviceLoadResult.fail());

        addDevice(failing);
        assertFalse(registry.mountDevices().wasSuccessful());

        verify(mounted, times(1)).unmount();
        verify(mounted, never()).dispose();

        removeDevice(failing);
        assertTrue(registry.mountDevices().wasSuccessful());
        verify(mounted, times(2)).mount(any());
    }

    @Test
    public void failedMountReleasesTheMemoryOfAlreadyMountedDevices() {
        final VMDevice mounted = mock(VMDevice.class);
        when(mounted.mount(any())).then(invocation -> {
            invocation.<VMContext>getArgument(0).getMemoryRangeAllocator()
                .claimMemoryRange(mock(MemoryMappedDevice.class));
            return VMDeviceLoadResult.success();
        });

        addDevice(mounted);
        assertTrue(registry.mountDevices().wasSuccessful());

        final VMDevice failing = mock(VMDevice.class);
        when(failing.mount(any())).thenReturn(VMDeviceLoadResult.fail());
        addDevice(failing);

        assertFalse(registry.mountDevices().wasSuccessful());

        removeDevice(failing);
        assertTrue(registry.mountDevices().wasSuccessful(), "the rolled back memory range must be claimable again");
    }

    @Test
    public void addingTheSameDeviceTwiceMountsItOnce() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        addDevice(device);
        addDevice(device);

        assertTrue(registry.mountDevices().wasSuccessful());
        verify(device, times(1)).mount(any());
    }

    private void addDevice(final VMDevice device) {
        busDevices.add(device);
        registry.rebuild(controller);
    }

    private void removeDevice(final VMDevice device) {
        busDevices.remove(device);
        registry.rebuild(controller);
    }

    // --------------------------------------------------------------------- //

    private static final class DeviceData {
        public VMContext context;
        public int interrupt;
        public MemoryMappedDevice device;
    }
}
