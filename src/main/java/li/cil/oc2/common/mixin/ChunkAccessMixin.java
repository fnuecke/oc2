/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import li.cil.oc2.common.ext.ChunkAccessExt;
import li.cil.oc2.common.util.ChunkUtils;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Tracks a "lazy unsaved" flag per {@link ChunkAccess} instance, to allow
 * marking chunks as needing saving just in time for "hard" saves.
 *
 * @see ChunkMapMixin <c>ChunkMapMixin</c> for more information
 * @see ChunkUtils
 */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements ChunkAccessExt {
    @Shadow
    protected volatile boolean unsaved;

    private volatile boolean lazyUnsaved;

    @Override
    public void setLazyUnsaved() {
        lazyUnsaved = true;
    }

    @Override
    public void applyAndClearLazyUnsaved() {
        if (!unsaved && lazyUnsaved) {
            unsaved = true;
        }
        lazyUnsaved = false;
    }
}
