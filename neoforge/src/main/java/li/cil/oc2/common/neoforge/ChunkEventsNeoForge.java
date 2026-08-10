/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.util.ChunkUtils;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayList;

@EventBusSubscriber(modid = API.MOD_ID)
public final class ChunkEventsNeoForge {
    @SubscribeEvent
    public static void handleChunkLoad(final ChunkEvent.Load event) {
        final LevelAccessor level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        ServerScheduler.onChunkLoad(level, event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void handleChunkUnload(final ChunkEvent.Unload event) {
        final LevelAccessor level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        final ChunkAccess chunk = event.getChunk();

        ServerScheduler.onChunkUnload(level, chunk.getPos());

        ChunkUtils.onChunkUnload(chunk);

        if (chunk instanceof final LevelChunk levelChunk) {
            for (final BlockEntity blockEntity : new ArrayList<>(levelChunk.getBlockEntities().values())) {
                if (blockEntity instanceof final ModBlockEntity modBlockEntity) {
                    modBlockEntity.onChunkUnloaded();
                }
            }
        }
    }

    private ChunkEventsNeoForge() {
    }
}
