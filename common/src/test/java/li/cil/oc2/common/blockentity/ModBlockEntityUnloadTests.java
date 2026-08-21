/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ModBlockEntityUnloadTests {
    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void levelUnloadFollowedByRemovalUnloadsOnlyOnce() {
        withBlockEntity(blockEntity -> {
            blockEntity.onWorldUnloaded();
            blockEntity.setRemoved();

            assertEquals(1, blockEntity.unloadCount);
        });
    }

    @Test
    public void chunkUnloadFollowedByRemovalUnloadsOnlyOnce() {
        withBlockEntity(blockEntity -> {
            blockEntity.onChunkUnloaded();
            blockEntity.setRemoved();

            assertEquals(1, blockEntity.unloadCount);
        });
    }

    @Test
    public void removalOnItsOwnStillUnloads() {
        withBlockEntity(blockEntity -> {
            blockEntity.setRemoved();

            assertEquals(1, blockEntity.unloadCount);
            assertTrue(blockEntity.lastWasRemove);
        });
    }

    // --------------------------------------------------------------------- //

    private static void withBlockEntity(final Consumer<CountingBlockEntity> test) {
        try (MockedStatic<Capabilities> ignored = mockStatic(Capabilities.class)) {
            final ServerLevel level = mock(ServerLevel.class);
            when(level.isClientSide()).thenReturn(false);

            final CountingBlockEntity blockEntity = new CountingBlockEntity();
            blockEntity.setLevel(level);

            test.accept(blockEntity);
        }
    }

    private static BlockEntityType<?> anyBlockEntityType() {
        final BlockEntityType<?> type = mock(BlockEntityType.class);
        when(type.isValid(any())).thenReturn(true);
        return type;
    }

    private static final class CountingBlockEntity extends ModBlockEntity {
        private int unloadCount;
        private boolean lastWasRemove;

        CountingBlockEntity() {
            super(anyBlockEntityType(), BlockPos.ZERO, Blocks.STONE.defaultBlockState());
        }

        @Override
        protected void unloadServer(final boolean isRemove) {
            super.unloadServer(isRemove);
            unloadCount++;
            lastWasRemove = isRemove;
        }
    }
}
