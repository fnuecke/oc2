/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IODeviceDescription;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.LifecycleAwareDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.api.bus.device.rpc.RPCBusContext;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.rpc.RedstoneInterfaceDocumentation;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@RPCDeviceDescription(typeNames = {"redstone"}, description = RedstoneInterfaceDocumentation.DEVICE)
@IODeviceDescription(name = "REDSTN", description = RedstoneInterfaceDocumentation.IO_DEVICE)
public final class RedstoneInterfaceBlockEntity extends ModBlockEntity implements LifecycleAwareDevice {
    public record RedstoneChangedEvent(String side, int value) {
        public static final String TYPE = "redstoneChanged";
    }

    private static final String OUTPUT_TAG_NAME = "output";

    private static final int GET_REDSTONE_INPUT_CODE = 1;
    private static final int GET_REDSTONE_OUTPUT_CODE = 2;
    private static final int SET_REDSTONE_OUTPUT_CODE = 3;

    // --------------------------------------------------------------------- //

    private final byte[] output = new byte[Constants.BLOCK_FACE_COUNT];
    private final byte[] input = new byte[Constants.BLOCK_FACE_COUNT];
    private final Set<RPCBusContext> contexts = new HashSet<>();

    // --------------------------------------------------------------------- //

    public RedstoneInterfaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.REDSTONE_INTERFACE.get(), pos, state);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putByteArray(OUTPUT_TAG_NAME, output);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final byte[] serializedOutput = tag.getByteArray(OUTPUT_TAG_NAME);
        System.arraycopy(serializedOutput, 0, output, 0, Math.min(serializedOutput.length, output.length));
    }

    @Override
    protected void loadServerInLoadedLevel() {
        super.loadServerInLoadedLevel();

        updateInputs(false);
    }

    public void handleNeighborChanged() {
        updateInputs(true);
    }

    @Override
    public void onDeviceMounted(final RPCBusContext context) {
        contexts.add(context);
    }

    @Override
    public void onDeviceUnmounted(final RPCBusContext context) {
        contexts.remove(context);
    }

    public int getOutputForDirection(final Direction direction) {
        final Direction localDirection = HorizontalBlockUtils.toLocal(getBlockState(), direction);
        assert localDirection != null;

        return output[localDirection.get3DDataValue()];
    }

    @Callback(synchronize = false,
        description = RedstoneInterfaceDocumentation.GET_REDSTONE_INPUT,
        returnValueDescription = RedstoneInterfaceDocumentation.GET_REDSTONE_INPUT_RESULT)
    public int getRedstoneInput(@Parameter(value = "side", description = RedstoneInterfaceDocumentation.SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        final int index = toLocalIndex(side);

        return input[index];
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

        final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), side);
        if (direction != null) {
            notifyNeighbor(direction);
        }

        setChanged();
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_REDSTONE_INPUT_CODE, synchronize = false, name = "getRedstoneInput",
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
        return HorizontalBlockUtils.toLocal(getBlockState(), side).get3DDataValue();
    }

    private int readInput(final Direction direction) {
        if (level == null) {
            return 0;
        }

        final BlockPos neighborPos = getBlockPos().relative(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return level.getSignal(neighborPos, direction);
    }

    private void updateInputs(final boolean sendEvents) {
        for (final Direction direction : Direction.values()) {
            final Direction localDirection = HorizontalBlockUtils.toLocal(getBlockState(), direction);
            assert localDirection != null;
            final int index = localDirection.get3DDataValue();

            final byte value = (byte) readInput(direction);
            if (value == input[index]) {
                continue;
            }
            input[index] = value;

            if (sendEvents) {
                final String side = Side.byIndex(index).name().toLowerCase(Locale.ROOT);
                final RedstoneChangedEvent event = new RedstoneChangedEvent(side, value);
                for (final RPCBusContext context : contexts) {
                    context.sendEvent(RedstoneChangedEvent.TYPE, event);
                }
            }
        }
    }

    private void notifyNeighbor(final Direction direction) {
        if (level == null) {
            return;
        }

        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        level.updateNeighborsAt(getBlockPos().relative(direction), getBlockState().getBlock());
    }
}
