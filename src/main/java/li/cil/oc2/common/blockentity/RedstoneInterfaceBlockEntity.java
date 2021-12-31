package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
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

    public RedstoneInterfaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.REDSTONE_INTERFACE.get(), pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putByteArray(OUTPUT_TAG_NAME, output);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final byte[] serializedOutput = tag.getByteArray(OUTPUT_TAG_NAME);
        System.arraycopy(serializedOutput, 0, output, 0, Math.min(serializedOutput.length, output.length));
    }

    public int getOutputForDirection(final Direction direction) {
        final Direction localDirection = HorizontalBlockUtils.toLocal(getBlockState(), direction);
        assert localDirection != null;

        return output[localDirection.get3DDataValue()];
    }

    @Callback(name = GET_REDSTONE_INPUT)
    public int getRedstoneInput(@Parameter(SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();

        if (level == null) {
            return 0;
        }

        final BlockPos pos = getBlockPos();
        final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), side);
        assert direction != null;

        final BlockPos neighborPos = pos.relative(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos.getX(), neighborPos.getZ());
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return level.getSignal(neighborPos, direction);
    }

    @Callback(name = GET_REDSTONE_OUTPUT, synchronize = false)
    public int getRedstoneOutput(@Parameter(SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();

        return output[side.get3DDataValue()];
    }

    @Callback(name = SET_REDSTONE_OUTPUT)
    public void setRedstoneOutput(@Parameter(SIDE) @Nullable final Side side, @Parameter(VALUE) final int value) {
        if (side == null) throw new IllegalArgumentException();

        final byte clampedValue = (byte) Mth.clamp(value, 0, 15);
        if (clampedValue == output[side.get3DDataValue()]) {
            return;
        }

        output[side.get3DDataValue()] = clampedValue;

        final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), side);
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
        if (level == null) {
            return;
        }

        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        level.updateNeighborsAt(getBlockPos().relative(direction), getBlockState().getBlock());
    }
}
