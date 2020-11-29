package li.cil.oc2.bus;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.device.IdentifiableDevice;
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
                controller.scan(world, new BlockPos(0, 0, 0)));
    }

    @Test
    public void scanCompletesWhenNoNeighbors() {
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);
        Assertions.assertEquals(DeviceBusControllerImpl.State.READY,
                controller.scan(world, new BlockPos(0, 0, 0)));
    }

    @Test
    public void scanSuccessfulWithLocalElement() {
        when(world.chunkExists(anyInt(), anyInt())).thenReturn(true);

        final TileEntity tileEntity = mock(TileEntity.class);
        when(world.getTileEntity(eq(new BlockPos(0, 0, 0)))).thenReturn(tileEntity);

        final DeviceBusElement busElement = mock(DeviceBusElement.class);
        when(tileEntity.getCapability(eq(busElementCapability), any())).thenReturn(LazyOptional.of(() -> busElement));

        final IdentifiableDevice device = mock(IdentifiableDevice.class);
        when(busElement.getLocalDevices()).thenReturn(Collections.singletonList(device));

        when(device.getUniqueId()).thenReturn(UUID.randomUUID());

        Assertions.assertEquals(DeviceBusControllerImpl.State.READY,
                controller.scan(world, new BlockPos(0, 0, 0)));

        verify(busElement).setController(controller);
        Assertions.assertTrue(controller.getDevices().contains(device));
    }
}
