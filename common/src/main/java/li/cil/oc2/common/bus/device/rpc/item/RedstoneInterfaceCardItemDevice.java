/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IODeviceDescription;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.bus.device.rpc.RedstoneInterfaceDocumentation;
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

@RPCDeviceDescription(typeNames = {"redstone"}, description = RedstoneInterfaceDocumentation.DEVICE)
@IODeviceDescription(name = "REDSTN", description = RedstoneInterfaceDocumentation.IO_DEVICE)
public final class RedstoneInterfaceCardItemDevice extends AbstractItemRPCDevice implements CapabilityProvider {
    private static final String OUTPUT_TAG_NAME = "output";

    private static final int GET_REDSTONE_INPUT_CODE = 1;
    private static final int GET_REDSTONE_OUTPUT_CODE = 2;
    private static final int SET_REDSTONE_OUTPUT_CODE = 3;

    // --------------------------------------------------------------------- //

    private final BlockEntity blockEntity;
    private final RedstoneEmitter[] capabilities;
    private final byte[] output = new byte[Constants.BLOCK_FACE_COUNT];

    // --------------------------------------------------------------------- //

    public RedstoneInterfaceCardItemDevice(final ItemStack identity, final BlockEntity blockEntity) {
        super(identity);
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

    @Callback(description = RedstoneInterfaceDocumentation.GET_REDSTONE_INPUT,
        returnValueDescription = RedstoneInterfaceDocumentation.GET_REDSTONE_INPUT_RESULT)
    public int getRedstoneInput(@Parameter(value = "side", description = RedstoneInterfaceDocumentation.SIDE) @Nullable final Side side) {
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

    @Callback(synchronize = false,
        description = RedstoneInterfaceDocumentation.GET_REDSTONE_OUTPUT,
        returnValueDescription = RedstoneInterfaceDocumentation.GET_REDSTONE_OUTPUT_RESULT)
    public int getRedstoneOutput(@Parameter(value = "side", description = RedstoneInterfaceDocumentation.SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        final int index = toLocalIndex(side);

        return output[index];
    }

    @Callback(description = RedstoneInterfaceDocumentation.SET_REDSTONE_OUTPUT)
    public void setRedstoneOutput(@Parameter(value = "side", description = RedstoneInterfaceDocumentation.SIDE) @Nullable final Side side,
                                  @Parameter(value = "value", description = RedstoneInterfaceDocumentation.SET_REDSTONE_OUTPUT_VALUE) final int value) {
        if (side == null) throw new IllegalArgumentException();
        final int index = toLocalIndex(side);

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

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_REDSTONE_INPUT_CODE, name = "getRedstoneInput",
        description = RedstoneInterfaceDocumentation.GET_REDSTONE_INPUT_IO,
        argumentsDescription = RedstoneInterfaceDocumentation.SIDE_IO,
        resultsDescription = RedstoneInterfaceDocumentation.LEVEL_IO)
    public void getRedstoneInputIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(getRedstoneInput(Side.byIndex(arguments.readU8())));
    }

    @IOCallback(value = GET_REDSTONE_OUTPUT_CODE, synchronize = false, name = "getRedstoneOutput",
        description = RedstoneInterfaceDocumentation.GET_REDSTONE_OUTPUT_IO,
        argumentsDescription = RedstoneInterfaceDocumentation.SIDE_IO,
        resultsDescription = RedstoneInterfaceDocumentation.LEVEL_IO)
    public void getRedstoneOutputIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(getRedstoneOutput(Side.byIndex(arguments.readU8())));
    }

    @IOCallback(value = SET_REDSTONE_OUTPUT_CODE, name = "setRedstoneOutput",
        description = RedstoneInterfaceDocumentation.SET_REDSTONE_OUTPUT_IO,
        argumentsDescription = RedstoneInterfaceDocumentation.SIDE_AND_LEVEL_IO)
    public void setRedstoneOutputIO(final IOInputStream arguments) throws IOException {
        final Side side = Side.byIndex(arguments.readU8());
        setRedstoneOutput(side, arguments.readU8());
    }

    // --------------------------------------------------------------------- //

    private int toLocalIndex(final Side side) {
        final Direction localDirection = HorizontalBlockUtils.toLocal(blockEntity.getBlockState(), side);
        assert localDirection != null;

        return localDirection.get3DDataValue();
    }

    private void notifyNeighbor(final Direction direction) {
        final Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        level.updateNeighborsAt(blockEntity.getBlockPos(), blockEntity.getBlockState().getBlock());
        level.updateNeighborsAt(blockEntity.getBlockPos().relative(direction), blockEntity.getBlockState().getBlock());
    }
}
