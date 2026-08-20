/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util.fabric;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LevelUtilsImpl {
    public static boolean fireBlockBreak(final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity);
    }

    public static boolean hasCorrectToolForDrops(final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
        return player.hasCorrectToolForDrops(state);
    }

    private LevelUtilsImpl() {
    }
}
