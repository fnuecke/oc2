/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resolves a block capability on each query, through the loader's cache for that position. Equal per position,
 * side and type, so devices built on top of it stay equal across re-scans.
 */
public final class CapabilityCache<T> {
    private final LevelAccessor level;
    private final BlockPos pos;
    @Nullable
    private final Direction side;
    private final CapabilityType<T> type;
    private final Supplier<T> source;

    // --------------------------------------------------------------------- //

    public CapabilityCache(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side,
                           final CapabilityType<T> type, final Supplier<T> source) {
        this.level = level;
        this.pos = pos.immutable();
        this.side = side;
        this.type = type;
        this.source = source;
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public T get() {
        return source.get();
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CapabilityCache<?> that = (CapabilityCache<?>) o;
        return level == that.level && pos.equals(that.pos) && side == that.side && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(level), pos, side, type);
    }
}
