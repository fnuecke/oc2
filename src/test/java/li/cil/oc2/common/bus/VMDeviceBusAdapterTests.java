package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.vm.VMDeviceBusAdapter;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.api.Board;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.api.memory.MemoryRangeAllocationStrategy;
import li.cil.sedna.memory.SimpleMemoryMap;
import li.cil.sedna.riscv.R5MemoryRangeAllocationStrategy;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public final class VMDeviceBusAdapterTests {
    private MemoryMap memoryMap;
    private InterruptController interruptController;
    private R5MemoryRangeAllocationStrategy allocationStrategy;
    private GlobalVMContext context;
    private VMDeviceBusAdapter adapter;

    @BeforeEach
    public void setupEach() {
        memoryMap = new SimpleMemoryMap();
        interruptController = new R5PlatformLevelInterruptController();
        allocationStrategy = new R5MemoryRangeAllocationStrategy();

        final Board board = mock(Board.class);
        when(board.getMemoryMap()).thenReturn(memoryMap);
        when(board.getInterruptController()).thenReturn(interruptController);
        when(board.getInterruptCount()).thenReturn(16);
        when(board.addDevice(any())).then(invocation -> {
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
        }).when(board).removeDevice(any());

        context = new GlobalVMContext(board);
        adapter = new VMDeviceBusAdapter(context);
    }

    @Test
    public void addingDeviceDoesNotMountDirectly() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        verify(device, never()).mount(any());
    }

    @Test
    public void addedDevicesHaveMountCalled() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());
        verify(device).mount(any());
    }

    @Test
    public void existingDevicesDoNotHaveMountCalledAgain() {
        final VMDevice device1 = mock(VMDevice.class);
        final VMDevice device2 = mock(VMDevice.class);
        when(device1.mount(any())).thenReturn(VMDeviceLoadResult.success());
        when(device2.mount(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device1));
        adapter.mountDevices();
        verify(device1).mount(any());

        adapter.addDevices(Collections.singleton(device2));
        adapter.mountDevices();
        verifyNoMoreInteractions(device1);
    }

    @Test
    public void deviceFailingMountDoesNotHaveUnmountOrDisposeCalled() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.fail());

        adapter.addDevices(Collections.singleton(device));
        assertFalse(adapter.mountDevices().wasSuccessful());
        verify(device).mount(any());
        verify(device, never()).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedIfOtherMountFails() {
        final VMDevice device1 = mock(VMDevice.class);
        final VMDevice device2 = mock(VMDevice.class);
        when(device1.mount(any())).thenReturn(VMDeviceLoadResult.success());
        when(device2.mount(any())).thenReturn(VMDeviceLoadResult.fail());

        adapter.addDevices(Collections.singleton(device1));
        adapter.addDevices(Collections.singleton(device2));
        adapter.mountDevices();

        verify(device1).mount(any());
        verify(device2).mount(any());
        verify(device1).unmount();
        verify(device1, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedWhenRemoved() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());

        adapter.removeDevices(Collections.singleton(device));
        verify(device).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void unmountedDevicesAreSilentlyRemoved() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        adapter.mountDevices();
        verify(device).mount(any());

        adapter.unmountDevices();
        verify(device).unmount();

        adapter.removeDevices(Collections.singleton(device));

        verify(device, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedButNotDisposedOnGlobalUnmount() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());

        adapter.unmountDevices();
        verify(device).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void unmountedDevicesAreNotUnmountedAndNotDisposedOnGlobalUnmount() {
        final VMDevice device = mock(VMDevice.class);

        adapter.addDevices(Collections.singleton(device));

        adapter.unmountDevices();
        verify(device, never()).unmount();
        verify(device, never()).dispose();
    }

    @Test
    public void mountedDevicesAreUnmountedAndDisposedOnGlobalDispose() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());
        adapter.addDevices(Collections.singleton(device));
        adapter.mountDevices();

        adapter.disposeDevices();
        verify(device).unmount();
        verify(device).dispose();
    }

    @Test
    public void unmountedDevicesAreNotUnmountedButDisposedOnGlobalDispose() {
        final VMDevice device = mock(VMDevice.class);
        adapter.addDevices(Collections.singleton(device));

        adapter.disposeDevices();
        verify(device, never()).unmount();
        verify(device).dispose();
    }

    @Test
    public void devicesHaveMountCalledAfterGlobalUnmount() {
        final VMDevice device = mock(VMDevice.class);
        when(device.mount(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        adapter.mountDevices();
        adapter.unmountDevices();

        assertTrue(adapter.mountDevices().wasSuccessful());
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

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());

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

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());
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

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());

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

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());

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

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());

        final int claimedInterruptMask = 1 << deviceData.interrupt;
        deviceData.context.getInterruptController().raiseInterrupts(claimedInterruptMask);

        assertTrue((interruptController.getRaisedInterrupts() & claimedInterruptMask) != 0);

        adapter.unmountDevices();

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

        adapter.addDevices(Collections.singleton(device));
        adapter.mountDevices();
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

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());

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

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.mountDevices().wasSuccessful());

        assertTrue(deviceData.context.getMemoryMap().getMemoryRange(deviceData.device).isPresent());

        adapter.unmountDevices();

        assertFalse(deviceData.context.getMemoryMap().getMemoryRange(deviceData.device).isPresent());
    }

    private static final class DeviceData {
        public VMContext context;
        public int interrupt;
        public MemoryMappedDevice device;
    }
}
