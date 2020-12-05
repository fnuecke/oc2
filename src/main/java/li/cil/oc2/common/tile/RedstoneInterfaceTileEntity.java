package li.cil.oc2.common.tile;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Collection;

import static java.util.Collections.singletonList;

public class RedstoneInterfaceTileEntity extends TileEntity implements NamedDevice {
    private static final int HORIZONTAL_DIRECTION_COUNT = 4;
    private static final String OUTPUT_NBT_TAG_NAME = "output";

    private final byte[] output = new byte[HORIZONTAL_DIRECTION_COUNT];

    public RedstoneInterfaceTileEntity() {
        super(OpenComputers.REDSTONE_INTERFACE_TILE_ENTITY.get());
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound = super.write(compound);
        compound.putByteArray(OUTPUT_NBT_TAG_NAME, output);
        return compound;
    }

    @Override
    public void read(final BlockState state, final CompoundNBT compound) {
        super.read(state, compound);
        final byte[] output = compound.getByteArray(OUTPUT_NBT_TAG_NAME);
        if (output.length == HORIZONTAL_DIRECTION_COUNT) {
            System.arraycopy(output, 0, this.output, 0, HORIZONTAL_DIRECTION_COUNT);
        }
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return singletonList("redstone");
    }

    @Callback
    public int getRedstoneInput(final int side) {
        if (side < 0 || side > 3) {
            throw new IllegalArgumentException("invalid side");
        }

        final World world = getWorld();
        if (world == null) {
            return 0;
        }

        final Direction facing = getBlockState().get(HorizontalBlock.HORIZONTAL_FACING);
        final int toGlobal = facing.getHorizontalIndex();
        final int rotatedIndex = (side + toGlobal) % HORIZONTAL_DIRECTION_COUNT;

        final BlockPos pos = getPos();
        final Direction direction = Direction.byHorizontalIndex(rotatedIndex);

        final BlockPos neighborPos = pos.offset(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos.getX(), neighborPos.getZ());
        if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return world.getRedstonePower(neighborPos, direction);
    }

    @Callback
    public int getRedstoneOutput(final int side) {
        if (side < 0 || side > 3) {
            throw new IllegalArgumentException("invalid side");
        }

        return output[side];
    }

    @Callback
    public void setRedstoneOutput(final int side, final int value) {
        if (side < 0 || side > 3) {
            throw new IllegalArgumentException("invalid side");
        }

        final int clamped = MathHelper.clamp(value, 0, 15);
        if (clamped == output[side]) {
            return;
        }

        output[side] = (byte) clamped;

        notifyNeighbors();
    }

    public int getOutputForDirection(final Direction direction) {
        if (direction.getAxis().getPlane() != Direction.Plane.HORIZONTAL) {
            return 0;
        }

        final Direction facing = getBlockState().get(HorizontalBlock.HORIZONTAL_FACING);
        final int toLocal = HORIZONTAL_DIRECTION_COUNT - facing.getHorizontalIndex();
        final int rotatedIndex = (direction.getHorizontalIndex() + toLocal) % HORIZONTAL_DIRECTION_COUNT;
        return output[rotatedIndex];
    }

    private void notifyNeighbors() {
        final World world = getWorld();
        if (world == null) {
            return;
        }

        world.notifyNeighborsOfStateChange(getPos(), getBlockState().getBlock());
        Direction.Plane.HORIZONTAL.iterator().forEachRemaining(direction ->
                world.notifyNeighborsOfStateChange(getPos().offset(direction), getBlockState().getBlock()));
    }
}
