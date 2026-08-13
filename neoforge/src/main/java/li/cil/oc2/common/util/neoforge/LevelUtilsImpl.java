/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;

public final class LevelUtilsImpl {
    public static boolean fireBlockBreak(final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
        return !CommonHooks.fireBlockBreak(level, GameType.DEFAULT_MODE, player, pos, state).isCanceled();
    }

    public static boolean hasCorrectToolForDrops(final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
        // For PlayerEvent.HarvestCheck.
        return player.hasCorrectToolForDrops(state, level, pos);
    }

    private LevelUtilsImpl() {
    }
}
