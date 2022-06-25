/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;

public record ChunkLocation(WeakReference<LevelAccessor> level, ChunkPos position) {
    public static ChunkLocation of(final LevelAccessor level, final BlockPos position) {
        return new ChunkLocation(new WeakReference<>(level), new ChunkPos(position));
    }

    public static ChunkLocation of(final LevelAccessor level, final ChunkPos position) {
        return new ChunkLocation(new WeakReference<>(level), position);
    }

    public Optional<LevelAccessor> tryGetLevel() {
        return Optional.ofNullable(level.get());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ChunkLocation that) {
            final LevelAccessor thisLevel = level.get();
            final LevelAccessor thatLevel = that.level.get();
            return Objects.equals(thisLevel, thatLevel) && Objects.equals(position, that.position);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level.get(), position);
    }
}
