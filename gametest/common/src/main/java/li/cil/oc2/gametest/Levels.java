/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class Levels {
    /**
     * As close to what a world reload does to a single block entity as we can get in a game test: save it,
     * remove it from the level, then bring a fresh instance back from that data.
     */
    public static void reloadBlockEntity(final GameTestHelper helper, final BlockPos relativePos) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(relativePos);
        final BlockState state = level.getBlockState(pos);

        final BlockEntity previous = level.getBlockEntity(pos);
        if (previous == null) {
            throw new GameTestAssertException("nothing to reload at " + relativePos);
        }

        final CompoundTag tag = previous.saveWithFullMetadata(level.registryAccess());
        level.removeBlockEntity(pos);

        final BlockEntity restored = BlockEntity.loadStatic(pos, state, tag, level.registryAccess());
        if (restored == null) {
            throw new GameTestAssertException("could not restore the block entity from its own data");
        }
        level.setBlockEntity(restored);
    }

    // --------------------------------------------------------------------- //

    private Levels() {
    }
}
