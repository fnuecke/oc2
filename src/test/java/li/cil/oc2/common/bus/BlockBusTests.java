/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class BlockBusTests {
    @SuppressWarnings("unchecked") private static final Capability<DeviceBusElement> DEVICE_BUS_ELEMENT_CAPABILITY = (Capability<DeviceBusElement>) mock(Capability.class);

    private FakeLevel fakeLevel;
    private LevelAccessor level;
    private MockedStatic<Capabilities> capabilitiesMock;

    @BeforeEach
    public void setupEach() {
        fakeLevel = new FakeLevel();
        level = fakeLevel.getLevel();
        capabilitiesMock = mockStatic(Capabilities.class);
        capabilitiesMock.when(Capabilities::deviceBusElement).thenReturn(DEVICE_BUS_ELEMENT_CAPABILITY);
    }

    @AfterEach
    public void teardownEach() {
        capabilitiesMock.close();
    }

    @Test
    public void busTouchingUnloadedChunkStaysIncomplete() {
        final BlockDeviceBusController busController = createBusController(new BlockPos(0, 0, 0));

        busController.scan();

        assertEquals(CommonDeviceBusController.BusState.INCOMPLETE, busController.getState());
    }

    @Test
    public void busNotTouchingUnloadedChunkCompletes() {
        final BlockDeviceBusController busController = createBusController(new BlockPos(8, 0, 8));

        busController.scan();

        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState());
    }

    @Test
    public void busControllerDetectsBusElements() {
        final BlockDeviceBusController busController = createBusController(new BlockPos(8, 0, 8));

        final DeviceBusElement busElement = createBusElement(new BlockPos(9, 0, 8));

        busController.scan();

        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState());

        assertTrue(busElement.getControllers().contains(busController));
        assertTrue(busController.getElements().contains(busElement));
    }

    private BlockDeviceBusController createBusController(final BlockPos pos) {
        final TestBlockDeviceBusElement busElement = new TestBlockDeviceBusElement(level, pos);
        final BlockEntity blockEntity = mock(BlockEntity.class);
        when(blockEntity.getBlockPos()).thenReturn(pos);
        when(blockEntity.getCapability(any(), any())).thenReturn(LazyOptional.empty());
        final BlockDeviceBusController busController = new BlockDeviceBusController(busElement, 0, blockEntity);

        fakeLevel.addBlockEntity(blockEntity);
        fakeLevel.setChunkLoaded(new ChunkPos(pos), true);

        return busController;
    }

    private AbstractBlockDeviceBusElement createBusElement(final BlockPos pos) {
        final TestBlockDeviceBusElement busElement = new TestBlockDeviceBusElement(level, pos);
        final BlockEntity blockEntity = mock(BlockEntity.class);
        when(blockEntity.getBlockPos()).thenReturn(pos);
        when(blockEntity.getCapability(any(), any())).thenReturn(LazyOptional.empty());
        when(blockEntity.getCapability(eq(Capabilities.deviceBusElement()), any())).thenReturn(LazyOptional.of(() -> busElement));

        fakeLevel.addBlockEntity(blockEntity);
        fakeLevel.setChunkLoaded(new ChunkPos(pos), true);

        return busElement;
    }

    private static final class FakeLevel {
        private final LevelAccessor level = mock(LevelAccessor.class);
        private final HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        private final HashSet<ChunkPos> loadedChunks = new HashSet<>();

        public FakeLevel() {
            when(level.getBlockEntity(any())).then(a -> blockEntities.get(a.<BlockPos>getArgument(0)));

            when(level.hasChunk(anyInt(), anyInt())).then(a -> {
                final int chunkX = a.getArgument(0);
                final int chunkZ = a.getArgument(1);
                return loadedChunks.contains(new ChunkPos(chunkX, chunkZ));
            });
        }

        public LevelAccessor getLevel() {
            return level;
        }

        public void addBlockEntity(final BlockEntity blockEntity) {
            blockEntities.put(blockEntity.getBlockPos(), blockEntity);
        }

        public void removeBlockEntity(final BlockPos pos) {
            blockEntities.remove(pos);
        }

        public void setChunkLoaded(final ChunkPos chunkPos, final boolean loaded) {
            if (loaded) {
                loadedChunks.add(chunkPos);
            } else {
                loadedChunks.remove(chunkPos);
            }
        }
    }

    private static final class TestBlockDeviceBusElement extends AbstractBlockDeviceBusElement {
        private final LevelAccessor level;
        private final BlockPos blockPos;

        public TestBlockDeviceBusElement(final LevelAccessor level, final BlockPos blockPos) {
            this.level = level;
            this.blockPos = blockPos;
        }

        @Override
        public LevelAccessor getLevel() {
            return level;
        }

        @Override
        public BlockPos getPosition() {
            return blockPos;
        }
    }
}
