package li.cil.oc2.common.util;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

import java.util.Objects;
import java.util.Optional;

public final class Location {
    public final IWorld world;
    public final BlockPos pos;

    public Location(final IWorld world, final BlockPos pos) {
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

    public static Optional<Location> of(final TileEntity tileEntity) {
        if (!tileEntity.isRemoved()) {
            return Optional.of(new Location(Objects.requireNonNull(tileEntity.getLevel()), tileEntity.getBlockPos()));
        } else {
            return Optional.empty();
        }
    }
}
