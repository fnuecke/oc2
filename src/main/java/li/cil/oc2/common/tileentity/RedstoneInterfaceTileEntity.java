package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.HorizontalDirectionalBlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Collection;

import static java.util.Collections.singletonList;

public final class RedstoneInterfaceBlockEntity extends BlockEntity implements NamedDevice, DocumentedDevice {
    private static final String OUTPUT_TAG_NAME = "output";

    private static final String GET_REDSTONE_INPUT = "getRedstoneInput";
    private static final String GET_REDSTONE_OUTPUT = "getRedstoneOutput";
    private static final String SET_REDSTONE_OUTPUT = "setRedstoneOutput";
    private static final String SIDE = "side";
    private static final String VALUE = "value";

    ///////////////////////////////////////////////////////////////////

    private final byte[] output = new byte[Constants.BLOCK_FACE_COUNT];

    ///////////////////////////////////////////////////////////////////

    public RedstoneInterfaceBlockEntity() {
        super(TileEntities.REDSTONE_INTERFACE_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public CompoundTag save(CompoundTag compound) {
        compound = super.save(compound);
        compound.putByteArray(OUTPUT_TAG_NAME, output);
        return compound;
    }

    @Override
    public void load(final BlockState state, final CompoundTag compound) {
        super.load(state, compound);
        final byte[] serializedOutput = compound.getByteArray(OUTPUT_TAG_NAME);
        System.arraycopy(serializedOutput, 0, output, 0, Math.min(serializedOutput.length, output.length));
    }

    public int getOutputForDirection(final Direction direction) {
        final Direction localDirection = HorizontalDirectionalBlockUtils.toLocal(getBlockState(), direction);
        assert localDirection != null;

        return output[localDirection.get3DDataValue()];
    }

    @Callback(name = GET_REDSTONE_INPUT)
    public int setSignal(@Parameter(SIDE) final Direction side) {
        final World world = getLevel();
        if (world == null) {
            return 0;
        }

        final BlockPos pos = getBlockPos();
        final Direction direction = HorizontalDirectionalBlockUtils.toGlobal(getBlockState(), side);
        assert direction != null;

        final BlockPos neighborPos = pos.relative(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos.getX(), neighborPos.getZ());
        if (!world.hasChunk(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return world.getSignal(neighborPos, direction);
    }

    @Callback(name = GET_REDSTONE_OUTPUT, synchronize = false)
    public int getSignal(@Parameter(SIDE) final Direction side) {
        return output[side.get3DDataValue()];
    }

    @Callback(name = SET_REDSTONE_OUTPUT)
    public void getSignal(@Parameter(SIDE) final Direction side, @Parameter(VALUE) final int value) {
        final byte clampedValue = (byte) MathHelper.clamp(value, 0, 15);
        if (clampedValue == output[side.get3DDataValue()]) {
            return;
        }

        output[side.get3DDataValue()] = clampedValue;

        final Direction direction = HorizontalDirectionalBlockUtils.toGlobal(getBlockState(), side);
        if (direction != null) {
            notifyNeighbor(direction);
        }

        setChanged();
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return singletonList("redstone");
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        visitor.visitCallback(GET_REDSTONE_INPUT)
                .description("Get the current redstone level received on the specified side. " +
                             "Note that if the current output level on the specified side is not " +
                             "zero, this will affect the measured level.\n" +
                             "Sides may be specified by name or zero-based index. Please note that" +
                             "the side depends on the orientation of the device.")
                .returnValueDescription("the current received level on the specified side.")
                .parameterDescription(SIDE, "the side to read the input level from.");

        visitor.visitCallback(GET_REDSTONE_OUTPUT)
                .description("Get the current redstone level transmitted on the specified side. " +
                             "This will return the value last set via setRedstoneOutput().\n" +
                             "Sides may be specified by name or zero-based index. Please note that" +
                             "the side depends on the orientation of the device.")
                .returnValueDescription("the current transmitted level on the specified side.")
                .parameterDescription(SIDE, "the side to read the output level from.");
        visitor.visitCallback(SET_REDSTONE_OUTPUT)
                .description("Set the new redstone level transmitted on the specified side.\n" +
                             "Sides may be specified by name or zero-based index. Please note that" +
                             "the side depends on the orientation of the device.")
                .parameterDescription(SIDE, "the side to write the output level to.")
                .parameterDescription(VALUE, "the output level to set, will be clamped to [0, 15].");
    }

    ///////////////////////////////////////////////////////////////////

    private void notifyNeighbor(final Direction direction) {
        final World world = getLevel();
        if (world == null) {
            return;
        }

        world.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        world.updateNeighborsAt(getBlockPos().relative(direction), getBlockState().getBlock());
    }
}
