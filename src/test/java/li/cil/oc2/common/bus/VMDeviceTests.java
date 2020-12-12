package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.vm.VirtualMachineDeviceBusAdapter;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.memory.SimpleMemoryMap;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public final class VMDeviceTests {
    private MemoryMap memoryMap;
    private InterruptController interruptController;
    private VirtualMachineDeviceBusAdapter adapter;

    @BeforeEach
    public void setupEach() {
        memoryMap = new SimpleMemoryMap();
        interruptController = new R5PlatformLevelInterruptController();
        adapter = new VirtualMachineDeviceBusAdapter(memoryMap, interruptController);
    }

    @Test
    public void addedDevicesHaveLoadCalled() {
        final VMDevice device1 = mock(VMDevice.class);
        final VMDevice device2 = mock(VMDevice.class);
        when(device1.load(any())).thenReturn(VMDeviceLoadResult.success());
        when(device2.load(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device1));
        assertTrue(adapter.load());
        verify(device1).load(any());

        adapter.addDevices(Collections.singleton(device2));
        assertTrue(adapter.load());

        verifyNoMoreInteractions(device1);
        verify(device2).load(any());
    }

    @Test
    public void removedDevicesHaveUnloadCalled() {
        final VMDevice device = mock(VMDevice.class);
        when(device.load(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.load());

        adapter.removeDevices(Collections.singleton(device));
        verify(device).unload();
    }

    @Test
    public void devicesHaveUnloadCalledOnGlobalUnload() {
        final VMDevice device = mock(VMDevice.class);
        when(device.load(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.load());

        adapter.unload();
        verify(device).unload();
    }

    @Test
    public void devicesHaveLoadCalledAfterGlobalUnload() {
        final VMDevice device = mock(VMDevice.class);
        when(device.load(any())).thenReturn(VMDeviceLoadResult.success());

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.load());
        verify(device).load(any());

        adapter.unload();
        verify(device).unload();

        assertTrue(adapter.load());
        verify(device, times(2)).load(any());
    }

    @Test
    public void deviceCanClaimInterrupts() {
        final VMDevice device = mock(VMDevice.class);
        when(device.load(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);
            final OptionalInt interrupt = context.getInterruptAllocator().claimInterrupt();
            assertTrue(interrupt.isPresent());
            return VMDeviceLoadResult.success();
        });

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.load());

        verify(device).load(any());
    }

    @Test
    public void deviceCannotClaimClaimedInterrupts() {
        final int claimedInterrupt = 1;

        final VMDevice device = mock(VMDevice.class);
        when(device.load(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);
            final OptionalInt interrupt = context.getInterruptAllocator().claimInterrupt(claimedInterrupt);
            assertTrue(interrupt.isPresent());
            assertNotEquals(claimedInterrupt, interrupt.getAsInt());
            return VMDeviceLoadResult.success();
        });

        adapter.claimInterrupt(claimedInterrupt);

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.load());
    }

    @Test
    public void deviceCanRaiseClaimedInterrupts() {
        final DeviceData deviceData = new DeviceData();
        final VMDevice device = mock(VMDevice.class);
        when(device.load(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);
            final OptionalInt interrupt = context.getInterruptAllocator().claimInterrupt();
            assertTrue(interrupt.isPresent());

            deviceData.context = context;
            deviceData.interrupt = interrupt.getAsInt();

            return VMDeviceLoadResult.success();
        });

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.load());

        verify(device).load(any());

        final int claimedInterruptMask = 1 << deviceData.interrupt;
        deviceData.context.getInterruptController().raiseInterrupts(claimedInterruptMask);

        assertTrue((interruptController.getRaisedInterrupts() & claimedInterruptMask) != 0);
    }

    @Test
    public void devicesCannotRaiseUnclaimedInterrupts() {
        final DeviceData deviceData = new DeviceData();
        final VMDevice device = mock(VMDevice.class);
        when(device.load(any())).thenAnswer(invocation -> {
            deviceData.context = invocation.getArgument(0);
            return VMDeviceLoadResult.success();
        });

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.load());

        verify(device).load(any());

        final int someInterruptMask = 0x1;
        assertThrows(IllegalArgumentException.class, () ->
                deviceData.context.getInterruptController().raiseInterrupts(someInterruptMask));
    }

    @Test
    public void unloadLowersClaimedInterrupts() {
        final DeviceData deviceData = new DeviceData();
        final VMDevice device = mock(VMDevice.class);
        when(device.load(any())).thenAnswer(invocation -> {
            final VMContext context = invocation.getArgument(0);
            final OptionalInt interrupt = context.getInterruptAllocator().claimInterrupt();
            assertTrue(interrupt.isPresent());

            deviceData.context = context;
            deviceData.interrupt = interrupt.getAsInt();

            return VMDeviceLoadResult.success();
        });

        adapter.addDevices(Collections.singleton(device));
        assertTrue(adapter.load());

        verify(device).load(any());

        final int claimedInterruptMask = 1 << deviceData.interrupt;
        deviceData.context.getInterruptController().raiseInterrupts(claimedInterruptMask);

        assertTrue((interruptController.getRaisedInterrupts() & claimedInterruptMask) != 0);

        adapter.unload();

        assertFalse((interruptController.getRaisedInterrupts() & claimedInterruptMask) != 0);
    }

    private static final class DeviceData {
        public VMContext context;
        public int interrupt;
    }
}
