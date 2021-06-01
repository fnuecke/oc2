package li.cil.oc2.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.Optional;

public final class Location {
    public final Level world;
    public final BlockPos pos;

    public Location(final Level world, final BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    public static Optional<Location> of(final Entity entity) {
        if (entity.isAlive()) {
            return Optional.of(new Location(entity.level, entity.blockPosition()));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Location> of(final BlockEntity tileEntity) {
        if (!tileEntity.isRemoved()) {
            return Optional.of(new Location(Objects.requireNonNull(tileEntity.getLevel()), tileEntity.getBlockPos()));
        } else {
            return Optional.empty();
        }
    }
}
