/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import li.cil.oc2.common.blockentity.ModBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Uniformly handles chunk unload logic in block entities across mod loaders.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    public abstract Map<BlockPos, BlockEntity> getBlockEntities();

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void unloadModBlockEntities(final CallbackInfo ci) {
        for (final BlockEntity blockEntity : getBlockEntities().values()) {
            if (blockEntity instanceof final ModBlockEntity modBlockEntity) {
                modBlockEntity.handleChunkUnloaded();
            }
        }
    }
}
