package li.cil.oc2.common.block.entity;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.init.TileEntities;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Collection;

import static java.util.Collections.singletonList;
import static li.cil.oc2.common.util.HorizontalBlockUtils.HORIZONTAL_DIRECTION_COUNT;

public class RedstoneInterfaceTileEntity extends TileEntity implements NamedDevice, DocumentedDevice {
    private static final String OUTPUT_NBT_TAG_NAME = "output";

    private static final String GET_REDSTONE_INPUT = "getRedstoneInput";
    private static final String GET_REDSTONE_OUTPUT = "getRedstoneOutput";
    private static final String SET_REDSTONE_OUTPUT = "setRedstoneOutput";
    private static final String SIDE = "side";
    private static final String VALUE = "value";

    ///////////////////////////////////////////////////////////////////

    private final byte[] output = new byte[HORIZONTAL_DIRECTION_COUNT];

    ///////////////////////////////////////////////////////////////////

    public RedstoneInterfaceTileEntity() {
        super(TileEntities.REDSTONE_INTERFACE_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

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

    @Callback(name = GET_REDSTONE_INPUT)
    public int getRedstoneInput(@Parameter(SIDE) final int side) {
        if (side < 0 || side > 3) {
            throw new IllegalArgumentException("invalid side");
        }

        final World world = getWorld();
        if (world == null) {
            return 0;
        }

        final BlockPos pos = getPos();
        final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), Direction.byHorizontalIndex(side));
        assert direction != null;

        final BlockPos neighborPos = pos.offset(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos.getX(), neighborPos.getZ());
        if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return world.getRedstonePower(neighborPos, direction);
    }

    @Callback(name = GET_REDSTONE_OUTPUT)
    public int getRedstoneOutput(@Parameter(SIDE) final int side) {
        if (side < 0 || side > 3) {
            throw new IllegalArgumentException("invalid side");
        }

        return output[side];
    }

    @Callback(name = SET_REDSTONE_OUTPUT)
    public void setRedstoneOutput(@Parameter(SIDE) final int side, @Parameter(VALUE) final int value) {
        if (side < 0 || side > 3) {
            throw new IllegalArgumentException("invalid side");
        }

        final byte clampedValue = (byte) MathHelper.clamp(value, 0, 15);
        if (clampedValue == output[side]) {
            return;
        }

        output[side] = clampedValue;

        notifyNeighbors();
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        visitor.visitCallback(GET_REDSTONE_INPUT)
                .description("Get the current redstone level received on the specified side. " +
                             "Note that if the current output level on the specified side is not " +
                             "zero, this will affect the measured level.\n" +
                             "Side indices start at zero. Please note the inscriptions on the device " +
                             "for which side corresponds to which index.")
                .returnValueDescription("the current received level on the specified side.")
                .parameterDescription(SIDE, "the side to read the input level from.");

        visitor.visitCallback(GET_REDSTONE_OUTPUT)
                .description("Get the current redstone level transmitted on the specified side. " +
                             "This will return the value last set via setRedstoneOutput().\n" +
                             "Side indices start at zero. Please note the inscriptions on the device " +
                             "for which side corresponds to which index.")
                .returnValueDescription("the current transmitted level on the specified side.")
                .parameterDescription(SIDE, "the side to read the output level from.");
        visitor.visitCallback(SET_REDSTONE_OUTPUT)
                .description("Set the new redstone level transmitted on the specified side.\n" +
                             "Side indices start at zero. Please note the inscriptions on the device " +
                             "for which side corresponds to which index.")
                .parameterDescription(SIDE, "the side to write the output level to.")
                .parameterDescription(VALUE, "the output level to set, will be clamped to [0, 15].");
    }

    public int getOutputForDirection(final Direction direction) {
        if (direction.getAxis().getPlane() != Direction.Plane.HORIZONTAL) {
            return 0;
        }

        final Direction localDirection = HorizontalBlockUtils.toLocal(getBlockState(), direction);
        assert localDirection != null;

        return output[localDirection.getHorizontalIndex()];
    }

    ///////////////////////////////////////////////////////////////////

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
