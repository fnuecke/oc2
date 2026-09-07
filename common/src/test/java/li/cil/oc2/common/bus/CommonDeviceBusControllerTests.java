/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.util.Invalidatable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class CommonDeviceBusControllerTests {
    private CommonDeviceBusController busController;
    private DeviceBusElement busControllerBusElement;

    @BeforeEach
    public void setupEach() {
        busControllerBusElement = mock(DeviceBusElement.class);
        when(busControllerBusElement.getLocalDevices()).thenReturn(emptyList());
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.empty());

        busController = new CommonDeviceBusController(busControllerBusElement, () -> 0, () -> Optional.of(ArchitectureType.RISCV));
    }

    @Test
    public void scanPendingWhenBlockEntityNotLoaded() {
        busController.scan();
        assertEquals(CommonDeviceBusController.BusState.INCOMPLETE, busController.getState());
    }

    @Test
    public void scanCompletesWhenNoNeighbors() {
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.emptyList()));

        busController.scan();
        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState());
    }

    @Test
    public void scanSuccessfulWithLocalElement() {
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.emptyList()));

        final RPCDevice device = mock(RPCDevice.class);
        when(busControllerBusElement.getLocalDevices()).thenReturn(singletonList(device));

        busController.scan();
        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState());

        verify(busControllerBusElement).addController(busController);
        assertTrue(busController.getDevices().contains(device));
    }

    @Test
    public void deviceScanWithoutChangesNotifiesNobody() {
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.emptyList()));

        final RPCDevice device = mock(RPCDevice.class);
        when(busControllerBusElement.getLocalDevices()).thenReturn(singletonList(device));

        busController.scan();

        final int[] beforeCount = new int[1];
        final int[] afterCount = new int[1];
        busController.onBeforeDeviceScan.add(() -> beforeCount[0]++);
        busController.onAfterDeviceScan.add(() -> afterCount[0]++);

        busController.scanDevices();

        assertEquals(0, beforeCount[0], "a scan finding the same devices must not pause the machine");
        assertEquals(0, afterCount[0], "a scan finding the same devices must not report a change");

        when(busControllerBusElement.getLocalDevices()).thenReturn(emptyList());
        busController.scanDevices();

        assertEquals(1, beforeCount[0]);
        assertEquals(1, afterCount[0]);
    }

    @Test
    public void disposedControllerStopsListeningToItsElements() {
        final DeviceBusElement busElement = mock(DeviceBusElement.class);
        final Invalidatable<DeviceBusElement> neighbor = Invalidatable.of(busElement);

        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.singleton(neighbor)));
        when(busElement.getNeighbors()).thenReturn(Optional.of(Collections.singleton(Invalidatable.of(busControllerBusElement))));

        busController.scan();
        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState());

        busController.dispose();
        neighbor.invalidate();

        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState(),
            "a disposed controller must not be called by an element it owned");
    }

    @Test
    public void scanSuccessfulWithMultipleElements() {
        // topology: controller <-> element 1 <-> element 2

        final DeviceBusElement busElement1 = mock(DeviceBusElement.class);
        final DeviceBusElement busElement2 = mock(DeviceBusElement.class);

        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.singleton(Invalidatable.of(busElement1))));
        when(busElement1.getNeighbors()).thenReturn(Optional.of(
            Set.of(Invalidatable.of(busControllerBusElement), Invalidatable.of(busElement2))));
        when(busElement2.getNeighbors()).thenReturn(Optional.of(Collections.singleton(Invalidatable.of(busElement1))));

        busController.scan();
        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState());

        verify(busElement1).addController(busController);
        verify(busElement2).addController(busController);
    }
}
