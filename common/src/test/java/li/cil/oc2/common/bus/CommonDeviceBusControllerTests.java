/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.util.Invalidatable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class CommonDeviceBusControllerTests {
    private CommonDeviceBusController busController;
    private DeviceBusElement busControllerBusElement;
    @Nullable
    private ArchitectureType architectureType;

    @BeforeEach
    public void setupEach() {
        busControllerBusElement = mock(DeviceBusElement.class);
        when(busControllerBusElement.getLocalDevices()).thenReturn(emptyList());
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.empty());

        architectureType = ArchitectureType.RISCV;

        busController = new CommonDeviceBusController(busControllerBusElement, () -> 0, () -> Optional.ofNullable(architectureType));
    }

    @Test
    public void scanIncompleteWhileNeighborsUnknown() {
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
    public void scanCollectsLocalDevices() {
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
    public void architectureChangesAreReported() {
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.emptyList()));

        final ArrayList<ArchitectureType> reported = new ArrayList<>();
        busController.onArchitectureChanged.add(reported::add);

        busController.scan();
        assertEquals(singletonList(ArchitectureType.RISCV), reported);

        busController.scheduleBusScan();
        busController.scan();
        assertEquals(1, reported.size(), "a scan finding the same architecture must not report a change");

        architectureType = null; //NOPMD - read by the scan below, through the supplier captured in setUp
        busController.scheduleBusScan();
        busController.scan();
        assertEquals(Arrays.asList(ArchitectureType.RISCV, null), reported, "removing the cpu must report a change");

        architectureType = ArchitectureType.Z80;
        busController.scheduleBusScan();
        busController.scan();
        assertEquals(Arrays.asList(ArchitectureType.RISCV, null, ArchitectureType.Z80), reported,
            "swapping the cpu must report a change");
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
