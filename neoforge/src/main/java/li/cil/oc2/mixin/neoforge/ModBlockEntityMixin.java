/* SPDX-License-Identifier: MIT */

package li.cil.oc2.mixin.neoforge;

import li.cil.oc2.common.blockentity.ModBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(ModBlockEntity.class)
public abstract class ModBlockEntityMixin extends BlockEntity {
    private ModBlockEntityMixin(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        @Nullable final AABB expanded = ((ModBlockEntity) (Object) this).getExpandedRenderBoundingBox();
        return expanded != null ? expanded : super.getRenderBoundingBox();
    }
}
