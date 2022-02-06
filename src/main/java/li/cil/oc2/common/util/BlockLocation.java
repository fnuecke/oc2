/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;

public record BlockLocation(WeakReference<LevelAccessor> level, BlockPos blockPos) {
    public static Optional<BlockLocation> of(final Entity entity) {
        if (entity.isAlive()) {
            return Optional.of(new BlockLocation(new WeakReference<>(entity.level), entity.blockPosition()));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<BlockLocation> of(final BlockEntity blockEntity) {
        if (!blockEntity.isRemoved()) {
            return Optional.of(new BlockLocation(new WeakReference<>(blockEntity.getLevel()), blockEntity.getBlockPos()));
        } else {
            return Optional.empty();
        }
    }

    public Optional<LevelAccessor> tryGetLevel() {
        return Optional.ofNullable(level.get());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof BlockLocation that) {
            final LevelAccessor thisLevel = level.get();
            final LevelAccessor thatLevel = that.level.get();
            return Objects.equals(thisLevel, thatLevel) && Objects.equals(blockPos, that.blockPos);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level.get(), blockPos);
    }
}
