package li.cil.oc2.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.Optional;

public record Location(LevelAccessor level, BlockPos pos) {
    public static Optional<Location> of(final Entity entity) {
        if (entity.isAlive()) {
            return Optional.of(new Location(entity.level, entity.blockPosition()));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Location> of(final BlockEntity blockEntity) {
        if (!blockEntity.isRemoved()) {
            return Optional.of(new Location(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos()));
        } else {
            return Optional.empty();
        }
    }
}
