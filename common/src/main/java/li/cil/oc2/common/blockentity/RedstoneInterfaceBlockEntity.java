/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOName;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.LifecycleAwareDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.rpc.RPCBusContext;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.Constants;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static java.util.Collections.singletonList;

@IOName("REDSTN")
public final class RedstoneInterfaceBlockEntity extends ModBlockEntity implements NamedDevice, DocumentedDevice, LifecycleAwareDevice {
    public record RedstoneChangedEvent(String side, int value) {
        public static final String TYPE = "redstoneChanged";
    }

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

    @Callback(name = GET_REDSTONE_INPUT, synchronize = false)
    public int getRedstoneInput(@Parameter(SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        final int index = toLocalIndex(side);

        return input[index];
    }

    @Callback(name = GET_REDSTONE_OUTPUT, synchronize = false)
    public int getRedstoneOutput(@Parameter(SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        final int index = toLocalIndex(side);

        return output[index];
    }

    @Callback(name = SET_REDSTONE_OUTPUT)
    public void setRedstoneOutput(@Parameter(SIDE) @Nullable final Side side, @Parameter(VALUE) final int value) {
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
                "Sides may be specified by name or zero-based index. Relative sides " +
                "(front, back, left, right) and indices depend on the orientation of the " +
                "device; absolute sides (north, south, west, east) do not.")
            .returnValueDescription("the current received level on the specified side.")
            .parameterDescription(SIDE, "the side to read the input level from.");

        visitor.visitCallback(GET_REDSTONE_OUTPUT)
            .description("Get the current redstone level transmitted on the specified side. " +
                "This will return the value last set via setRedstoneOutput().\n" +
                "Sides may be specified by name or zero-based index. Relative sides " +
                "(front, back, left, right) and indices depend on the orientation of the " +
                "device; absolute sides (north, south, west, east) do not.")
            .returnValueDescription("the current transmitted level on the specified side.")
            .parameterDescription(SIDE, "the side to read the output level from.");
        visitor.visitCallback(SET_REDSTONE_OUTPUT)
            .description("Set the new redstone level transmitted on the specified side.\n" +
                "Sides may be specified by name or zero-based index. Relative sides " +
                "(front, back, left, right) and indices depend on the orientation of the " +
                "device; absolute sides (north, south, west, east) do not.")
            .parameterDescription(SIDE, "the side to write the output level to.")
            .parameterDescription(VALUE, "the output level to set, will be clamped to [0, 15].");
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_REDSTONE_INPUT_CODE, synchronize = false)
    public void getRedstoneInputIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(getRedstoneInput(Side.byIndex(arguments.readU8())));
    }

    @IOCallback(value = GET_REDSTONE_OUTPUT_CODE, synchronize = false)
    public void getRedstoneOutputIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(getRedstoneOutput(Side.byIndex(arguments.readU8())));
    }

    @IOCallback(SET_REDSTONE_OUTPUT_CODE)
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
