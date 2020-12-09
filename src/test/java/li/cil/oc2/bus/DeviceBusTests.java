package li.cil.oc2.bus;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.bus.TileEntityDeviceBusController;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DeviceBusTests {
    private static final BlockPos CONTROLLER_POS = new BlockPos(0, 0, 0);

    @Mock
    private Capability<DeviceBusElement> busElementCapability;
    private World world;
    private TileEntity busControllerTileEntity;
    private TileEntityDeviceBusController busController;
    private DeviceBusElement busControllerBusElement;

    @BeforeAll
    public static void setup() {
        // Gotta go through regular MC bootstrapping first because otherwise class
        // load order may lead to errors because static fields reference each other.
        Bootstrap.register();
    }

    @BeforeEach
    public void setupEach() {
        MockitoAnnotations.initMocks(this);
        Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY = busElementCapability;

        world = mock(World.class);

        busControllerTileEntity = mock(TileEntity.class);
        when(busControllerTileEntity.getWorld()).thenReturn(world);
        when(busControllerTileEntity.getPos()).thenReturn(CONTROLLER_POS);
        when(busControllerTileEntity.getCapability(any(), any())).thenReturn(LazyOptional.empty());

        when(world.getTileEntity(CONTROLLER_POS)).thenReturn(busControllerTileEntity);

        busControllerBusElement = mock(DeviceBusElement.class);
        when(busControllerTileEntity.getCapability(eq(busElementCapability), any()))
                .thenReturn(LazyOptional.of(() -> busControllerBusElement));
        when(busControllerBusElement.getLocalDevices()).thenReturn(emptyList());

        busController = new TestBusController();
    }

    @Test
    public void scanPendingWhenTileEntityNotLoaded() {
        busController.scan();
        assertEquals(TileEntityDeviceBusController.State.INCOMPLETE, busController.getState());
    }

    @Test
    public void scanCompletesWhenNoNeighbors() {
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);
        busController.scan();
        assertEquals(TileEntityDeviceBusController.State.READY, busController.getState());
    }

    @Test
    public void scanSuccessfulWithLocalElement() {
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);

        final RPCDevice device = mock(RPCDevice.class);
        when(busControllerBusElement.getLocalDevices()).thenReturn(singletonList(device));

        busController.scan();
        assertEquals(TileEntityDeviceBusController.State.READY, busController.getState());

        verify(busControllerBusElement).addController(busController);
        assertTrue(busController.getDevices().contains(device));
    }

    @Test
    public void scanSuccessfulWithMultipleElements() {
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);

        final DeviceBusElement busElement1 = mockBusElement(CONTROLLER_POS.west());
        final DeviceBusElement busElement2 = mockBusElement(CONTROLLER_POS.west().west());

        busController.scan();
        assertEquals(TileEntityDeviceBusController.State.READY, busController.getState());

        verify(busElement1).addController(busController);
        verify(busElement2).addController(busController);
    }

    private DeviceBusElement mockBusElement(final BlockPos pos) {
        final TileEntity tileEntity = mock(TileEntity.class);
        when(world.getTileEntity(pos)).thenReturn(tileEntity);
        when(tileEntity.getCapability(any(), any())).thenReturn(LazyOptional.empty());

        final DeviceBusElement busElement = mock(DeviceBusElement.class);
        when(tileEntity.getCapability(eq(busElementCapability), any())).thenReturn(LazyOptional.of(() -> busElement));
        when(busElement.getLocalDevices()).thenReturn(emptyList());

        return busElement;
    }

    private final class TestBusController extends TileEntityDeviceBusController {
        public TestBusController() {
            super(busControllerTileEntity);
        }
    }
}
