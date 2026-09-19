/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.util;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

public final class Chunks {
    public static ChunkPos hold(final GameTestHelper helper, final BlockPos relativePos) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunkPos = new ChunkPos(helper.absolutePos(relativePos));

        level.setChunkForced(chunkPos.x, chunkPos.z, true);
        level.getChunk(chunkPos.x, chunkPos.z);

        return chunkPos;
    }

    public static void hold(final GameTestHelper helper, final ChunkPos chunkPos) {
        final ServerLevel level = helper.getLevel();
        level.setChunkForced(chunkPos.x, chunkPos.z, true);
        level.getChunk(chunkPos.x, chunkPos.z);
    }

    public static void release(final GameTestHelper helper, final ChunkPos chunkPos) {
        helper.getLevel().setChunkForced(chunkPos.x, chunkPos.z, false);
    }

    public static void assertTicking(final GameTestHelper helper, final BlockPos relativePos) {
        if (!helper.getLevel().isPositionEntityTicking(helper.absolutePos(relativePos))) {
            throw new GameTestAssertException("chunk of " + relativePos + " is not ticking yet");
        }
    }

    public static void assertUnloaded(final GameTestHelper helper, final ChunkPos chunkPos) {
        if (helper.getLevel().hasChunk(chunkPos.x, chunkPos.z)) {
            throw new GameTestAssertException("chunk " + chunkPos + " is still loaded");
        }
    }

    public static void prepare(final GameTestHelper helper, final BlockPos relativePos) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(relativePos);

        for (final BlockPos target : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 2, 1))) {
            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        }
        for (final BlockPos target : BlockPos.betweenClosed(pos.offset(-1, -2, -1), pos.offset(1, -2, 1))) {
            level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        }
    }

    // --------------------------------------------------------------------- //

    private Chunks() {
    }
}
