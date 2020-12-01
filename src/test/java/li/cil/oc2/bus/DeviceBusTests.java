package li.cil.oc2.bus;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.device.Device;
import li.cil.oc2.common.bus.DeviceBusControllerImpl;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.sedna.api.device.serial.SerialDevice;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DeviceBusTests {
    private static final BlockPos CONTROLLER_POS = new BlockPos(0, 0, 0);

    @Mock
    private Capability<DeviceBusElement> busElementCapability;
    private World world;
    private SerialDevice serialDevice;
    private DeviceBusControllerImpl controller;

    @BeforeEach
    public void setupEach() {
        MockitoAnnotations.initMocks(this);
        Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY = busElementCapability;

        world = mock(World.class);
        serialDevice = mock(SerialDevice.class);
        controller = new DeviceBusControllerImpl(serialDevice);
    }

    @Test
    public void scanPendingWhenTileEntityNotLoaded() {
        Assertions.assertEquals(DeviceBusControllerImpl.State.SCAN_PENDING,
                controller.scan(world, CONTROLLER_POS));
    }

    @Test
    public void scanCompletesWhenNoNeighbors() {
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);
        Assertions.assertEquals(DeviceBusControllerImpl.State.READY,
                controller.scan(world, CONTROLLER_POS));
    }

    @Test
    public void scanSuccessfulWithLocalElement() {
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);

        final TileEntity tileEntity = mock(TileEntity.class);
        when(world.getTileEntity(eq(CONTROLLER_POS))).thenReturn(tileEntity);

        final DeviceBusElement busElement = mock(DeviceBusElement.class);
        when(tileEntity.getCapability(eq(busElementCapability), any())).thenReturn(LazyOptional.of(() -> busElement));

        final Device device = mock(Device.class);
        when(busElement.getLocalDevices()).thenReturn(Collections.singletonList(device));

        when(device.getUniqueIdentifier()).thenReturn(UUID.randomUUID());

        Assertions.assertEquals(DeviceBusControllerImpl.State.READY,
                controller.scan(world, CONTROLLER_POS));

        verify(busElement).setController(controller);
        Assertions.assertTrue(controller.getDevices().contains(device));
    }

    @Test
    public void scanSuccessfulWithMultipleElements() {
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);

        final TileEntity tileEntityController = mock(TileEntity.class);
        when(world.getTileEntity(eq(CONTROLLER_POS))).thenReturn(tileEntityController);

        final DeviceBusElement busElementController = mock(DeviceBusElement.class);
        when(tileEntityController.getCapability(eq(busElementCapability), any())).thenReturn(LazyOptional.of(() -> busElementController));

        final TileEntity tileEntityBusElement1 = mock(TileEntity.class);
        when(world.getTileEntity(eq(CONTROLLER_POS.west()))).thenReturn(tileEntityBusElement1);

        final DeviceBusElement busElement1 = mock(DeviceBusElement.class);
        when(tileEntityBusElement1.getCapability(eq(busElementCapability), any())).thenReturn(LazyOptional.of(() -> busElement1));
        when(busElement1.getLocalDevices()).thenReturn(Collections.emptyList());

        final TileEntity tileEntityBusElement2 = mock(TileEntity.class);
        when(world.getTileEntity(eq(CONTROLLER_POS.west().west()))).thenReturn(tileEntityBusElement2);

        final DeviceBusElement busElement2 = mock(DeviceBusElement.class);
        when(tileEntityBusElement2.getCapability(eq(busElementCapability), any())).thenReturn(LazyOptional.of(() -> busElement2));
        when(busElement2.getLocalDevices()).thenReturn(Collections.emptyList());

        Assertions.assertEquals(DeviceBusControllerImpl.State.READY,
                controller.scan(world, CONTROLLER_POS));

        verify(busElement1).setController(controller);
        verify(busElement2).setController(controller);
    }
}
