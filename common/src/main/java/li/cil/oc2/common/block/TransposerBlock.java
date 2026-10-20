/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

public final class TransposerBlock extends Block implements EntityBlock {
    public TransposerBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        ModBlockEntity.markDestroyed(level, pos, state, newState, movedByPiston);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // --------------------------------------------------------------------- //
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.TRANSPOSER.get().create(pos, state);
    }
}
