/* SPDX-License-Identifier: MIT */

package li.cil.oc2.mixin.neoforge;

import li.cil.oc2.common.block.BusCableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BusCableBlock.class)
public abstract class BusCableBlockMixin extends Block {
    private BusCableBlockMixin(final Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult hit, final LevelReader level, final BlockPos pos, final Player player) {
        final ItemStack stack = BusCableBlock.getPickedStack(level, pos, player);
        if (!stack.isEmpty()) {
            return stack;
        }

        return super.getCloneItemStack(state, hit, level, pos, player);
    }
}
