package li.cil.oc2.common.device;

import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockDeviceQueryImpl implements BlockDeviceQuery {
    private final World world;
    private final BlockPos pos;
    @Nullable private final Direction side;

    public BlockDeviceQueryImpl(final World world, final BlockPos pos, @Nullable final Direction side) {
        this.world = world;
        this.pos = pos;
        this.side = side;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public BlockPos getQueryPosition() {
        return pos;
    }

    @Nullable
    @Override
    public Direction getQuerySide() {
        return side;
    }
}
