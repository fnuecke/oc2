/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.MinecraftBootstrap;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MinecraftBootstrap.class)
public class ModBlockEntityUnloadTests {
    private static final Map<String, Consumer<CountingBlockEntity>> UNLOADS = Map.of(
        "level unload", CountingBlockEntity::onWorldUnloaded,
        "chunk unload", CountingBlockEntity::handleChunkUnloaded);

    @Test
    public void unloadFollowedByRemovalUnloadsOnlyOnce() {
        UNLOADS.forEach((path, unload) -> withBlockEntity(blockEntity -> {
            unload.accept(blockEntity);
            blockEntity.setRemoved();

            assertEquals(1, blockEntity.unloadCount, path + " followed by removal should unload once");
        }));
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

    @SuppressWarnings("try")
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
