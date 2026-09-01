/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOName;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityProvider;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@IOName("REDSTN")
public final class RedstoneInterfaceCardItemDevice extends AbstractItemRPCDevice implements DocumentedDevice, CapabilityProvider {
    private static final String OUTPUT_TAG_NAME = "output";

    private static final String GET_REDSTONE_INPUT = "getRedstoneInput";
    private static final String GET_REDSTONE_OUTPUT = "getRedstoneOutput";
    private static final String SET_REDSTONE_OUTPUT = "setRedstoneOutput";
    private static final String SIDE = "side";
    private static final String VALUE = "value";
    private static final int GET_REDSTONE_INPUT_CODE = 1;
    private static final int GET_REDSTONE_OUTPUT_CODE = 2;
    private static final int SET_REDSTONE_OUTPUT_CODE = 3;

    // --------------------------------------------------------------------- //

    private final BlockEntity blockEntity;
    private final RedstoneEmitter[] capabilities;
    private final byte[] output = new byte[Constants.BLOCK_FACE_COUNT];

    // --------------------------------------------------------------------- //

    public RedstoneInterfaceCardItemDevice(final ItemStack identity, final BlockEntity blockEntity) {
        super(identity, "redstone");
        this.blockEntity = blockEntity;

        capabilities = new RedstoneEmitter[Constants.BLOCK_FACE_COUNT];
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            final int indexForClosure = i;
            capabilities[i] = () -> output[indexForClosure];
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (capability == Capabilities.REDSTONE_EMITTER && side != null) {
            return (T) capabilities[side.get3DDataValue()];
        }

        return null;
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putByteArray(OUTPUT_TAG_NAME, output);
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        final byte[] serializedOutput = tag.getByteArray(OUTPUT_TAG_NAME);
        System.arraycopy(serializedOutput, 0, output, 0, Math.min(serializedOutput.length, output.length));
    }

    @Callback(name = GET_REDSTONE_INPUT)
    public int getRedstoneInput(@Parameter(SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();

        final Level level = blockEntity.getLevel();
        if (level == null) {
            return 0;
        }

        final BlockPos pos = blockEntity.getBlockPos();
        final Direction direction = HorizontalBlockUtils.toGlobal(blockEntity.getBlockState(), side);
        assert direction != null;

        final BlockPos neighborPos = pos.relative(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return level.getSignal(neighborPos, direction);
    }

    @Callback(name = GET_REDSTONE_OUTPUT, synchronize = false)
    public int getRedstoneOutput(@Parameter(SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        final int index = side.getDirection().get3DDataValue();

        return output[index];
    }

    @Callback(name = SET_REDSTONE_OUTPUT)
    public void setRedstoneOutput(@Parameter(SIDE) @Nullable final Side side, @Parameter(VALUE) final int value) {
        if (side == null) throw new IllegalArgumentException();
        final int index = side.getDirection().get3DDataValue();

        final byte clampedValue = (byte) Mth.clamp(value, 0, 15);
        if (clampedValue == output[index]) {
            return;
        }

        output[index] = clampedValue;

        final Direction direction = HorizontalBlockUtils.toGlobal(blockEntity.getBlockState(), side);
        if (direction != null) {
            notifyNeighbor(direction);
        }
    }

    @Override
    public void getDeviceDocumentation(final DocumentedDevice.DeviceVisitor visitor) {
        visitor.visitCallback(GET_REDSTONE_INPUT)
            .description("Get the current redstone level received on the specified side. " +
                "Note that if the current output level on the specified side is not " +
                "zero, this will affect the measured level.\n" +
                "Sides may be specified by name or zero-based index. Please note that " +
                "the side depends on the orientation of the device's container.")
            .returnValueDescription("the current received level on the specified side.")
            .parameterDescription(SIDE, "the side to read the input level from.");

        visitor.visitCallback(GET_REDSTONE_OUTPUT)
            .description("Get the current redstone level transmitted on the specified side. " +
                "This will return the value last set via setRedstoneOutput().\n" +
                "Sides may be specified by name or zero-based index. Please note that " +
                "the side depends on the orientation of the device's container.")
            .returnValueDescription("the current transmitted level on the specified side.")
            .parameterDescription(SIDE, "the side to read the output level from.");
        visitor.visitCallback(SET_REDSTONE_OUTPUT)
            .description("Set the new redstone level transmitted on the specified side.\n" +
                "Sides may be specified by name or zero-based index. Please note that " +
                "the side depends on the orientation of the device's container.")
            .parameterDescription(SIDE, "the side to write the output level to.")
            .parameterDescription(VALUE, "the output level to set, will be clamped to [0, 15].");
    }

    // --------------------------------------------------------------------- //

    @IOCallback(GET_REDSTONE_INPUT_CODE)
    public void getRedstoneInputIO(final InputStream arguments, final OutputStream results) throws IOException {
        results.write(getRedstoneInput(Side.byIndex(arguments.read())));
    }

    @IOCallback(value = GET_REDSTONE_OUTPUT_CODE, synchronize = false)
    public void getRedstoneOutputIO(final InputStream arguments, final OutputStream results) throws IOException {
        results.write(getRedstoneOutput(Side.byIndex(arguments.read())));
    }

    @IOCallback(SET_REDSTONE_OUTPUT_CODE)
    public void setRedstoneOutputIO(final InputStream arguments) throws IOException {
        final Side side = Side.byIndex(arguments.read());
        final int value = arguments.read();
        if (value < 0) {
            throw new IllegalArgumentException("Missing output level.");
        }
        setRedstoneOutput(side, value);
    }

    // --------------------------------------------------------------------- //

    private void notifyNeighbor(final Direction direction) {
        final Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        level.updateNeighborsAt(blockEntity.getBlockPos(), blockEntity.getBlockState().getBlock());
        level.updateNeighborsAt(blockEntity.getBlockPos().relative(direction), blockEntity.getBlockState().getBlock());
    }
}
