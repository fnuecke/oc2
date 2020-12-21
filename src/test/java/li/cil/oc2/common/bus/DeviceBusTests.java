package li.cil.oc2.common.bus;

import alexiil.mc.lib.attributes.Attribute;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class DeviceBusTests {
    @Mock
    private Attribute<DeviceBusElement> busElementCapability;
    private AbstractDeviceBusController busController;
    private DeviceBusElement busControllerBusElement;

    @BeforeAll
    public static void setup() {
        // Gotta go through regular MC bootstrapping first because otherwise class
        // load order may lead to errors because static fields reference each other.
        Bootstrap.initialize();
    }

    @BeforeEach
    public void setupEach() {
        MockitoAnnotations.initMocks(this);
        Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY = busElementCapability;

        busControllerBusElement = mock(DeviceBusElement.class);
        when(busControllerBusElement.getLocalDevices()).thenReturn(emptyList());
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.empty());

        busController = new TestBusController();
    }

    @Test
    public void scanPendingWhenTileEntityNotLoaded() {
        busController.scan();
        assertEquals(AbstractDeviceBusController.BusState.INCOMPLETE, busController.getState());
    }

    @Test
    public void scanCompletesWhenNoNeighbors() {
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.emptyList()));

        busController.scan();
        assertEquals(AbstractDeviceBusController.BusState.READY, busController.getState());
    }

    @Test
    public void scanSuccessfulWithLocalElement() {
        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.emptyList()));

        final RPCDevice device = mock(RPCDevice.class);
        when(busControllerBusElement.getLocalDevices()).thenReturn(singletonList(device));

        busController.scan();
        assertEquals(AbstractDeviceBusController.BusState.READY, busController.getState());

        verify(busControllerBusElement).addController(busController);
        assertTrue(busController.getDevices().contains(device));
    }

    @Test
    public void scanSuccessfulWithMultipleElements() {
        // topology: controller <-> element 1 <-> element 2

        final DeviceBusElement busElement1 = mock(DeviceBusElement.class);
        final DeviceBusElement busElement2 = mock(DeviceBusElement.class);

        when(busControllerBusElement.getNeighbors()).thenReturn(Optional.of(Collections.singleton(busElement1)));
        when(busElement1.getNeighbors()).thenReturn(Optional.of(Collections.singleton(busControllerBusElement)));

        when(busElement1.getNeighbors()).thenReturn(Optional.of(Collections.singleton(busElement2)));
        when(busElement2.getNeighbors()).thenReturn(Optional.of(Collections.singleton(busElement1)));

        busController.scan();
        assertEquals(AbstractDeviceBusController.BusState.READY, busController.getState());

        verify(busElement1).addController(busController);
        verify(busElement2).addController(busController);
    }

    private final class TestBusController extends AbstractDeviceBusController {
        public TestBusController() {
            super(busControllerBusElement);
        }
    }
}
