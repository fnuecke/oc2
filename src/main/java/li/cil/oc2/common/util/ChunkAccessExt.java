package li.cil.oc2.common.util;

import li.cil.oc2.common.mixin.ChunkMapMixin;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Interface injected into the {@link ChunkAccess} class.
 * <p>
 * Tracks a "lazy unsaved" flag, which is converted into the regular unsaved flag
 * before certain manual save operations.
 *
 * @see ChunkUtils
 * @see ChunkMapMixin
 */
public interface ChunkAccessExt {
    /**
     * Set the lazy unsaved flag for this instance.
     * <p>
     * This method is used by the utility methods in {@link ChunkUtils}.
     */
    void setLazyUnsaved();

    /**
     * Set the unsaved flag for this instance, if the lazy unsaved flag is set,
     * then clears the lazy unsaved flag.
     * <p>
     * This method is invoked from mixins injected into the {@link ChunkMap} class.
     */
    void applyAndClearLazyUnsaved();
}
