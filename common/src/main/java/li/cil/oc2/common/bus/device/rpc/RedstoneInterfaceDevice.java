/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

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
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@RPCDeviceDescription(typeNames = {"redstone"}, description = """
    Provided by the [redstone interface](../block/redstone_interface.md) block and the [redstone interface card](../item/redstone_interface_card.md).

    ### Sides
    The side parameter in the following methods comes in two forms.

    Relative sides turn with the block, or with the computer holding the card. Each face of the redstone interface block has an indicator for convenience; the primary face is the one with a single marking. When looking at the primary face:
    - `front` is the face we are looking at.
    - `back` is the face behind the block.
    - `left` is the face to our left.
    - `right` is the face to our right.

    Absolute sides always mean the same direction in the world, no matter how the block is placed: `north`, `south`, `west` and `east`.

    `up` and `down` are the top and bottom faces, and mean the same thing either way.

    Sides may also be given as a number instead of a name. Numbers are relative: `0` is `down`, `1` is `up`, `2` is `back`, `3` is `front`, `4` is `left` and `5` is `right`.

    ### Events
    The device sends `redstoneChanged` when the received signal on a side changes, so a program can wait for it instead of polling. For example:
    `local e = r:waitEvent(nil, "redstoneChanged")`
    `print(e.data.side, e.data.value)`
    - `side` is the relative name of the side, as in the "Sides" section.
    - `value` is the new signal strength.

    The device's own output counts towards the received signal, so setting an output may send this event as well.""")
@IODeviceDescription(name = "REDSTN", description = """
    Sides are numbered as in the "Sides" section above, and levels are in [0, 15].

    `REDSTN.Z80` on the CP/M boot disk is an example consumer of the API. The [mid-level API](../mlapi.md) entry explains how to build and run it.""")
public final class RedstoneInterfaceDevice implements LifecycleAwareDevice {
    public record RedstoneChangedEvent(String side, int value) {
        public static final String TYPE = "redstoneChanged";
    }

    private static final String OUTPUT_TAG_NAME = "output";

    private static final int GET_REDSTONE_INPUT_CODE = 1;
    private static final int GET_REDSTONE_OUTPUT_CODE = 2;
    private static final int SET_REDSTONE_OUTPUT_CODE = 3;

    private static final String SIDE = "the side, by name (`front`, `back`, `left`, `right`, `up`, `down`, `north`, `south`, `west`, `east`) or by relative index.";
    private static final String SIDE_IO = "one byte, the side.";
    private static final String LEVEL_IO = "one byte, the level.";

    // --------------------------------------------------------------------- //

    private final BlockEntity host;
    private final byte[] output = new byte[Constants.BLOCK_FACE_COUNT];
    private final byte[] input = new byte[Constants.BLOCK_FACE_COUNT];
    private final Set<RPCBusContext> contexts = new HashSet<>();

    // --------------------------------------------------------------------- //

    public RedstoneInterfaceDevice(final BlockEntity host) {
        this.host = host;
    }

    // --------------------------------------------------------------------- //

    public void save(final CompoundTag tag) {
        tag.putByteArray(OUTPUT_TAG_NAME, output);
    }

    public void load(final CompoundTag tag) {
        final byte[] serializedOutput = tag.getByteArray(OUTPUT_TAG_NAME);
        System.arraycopy(serializedOutput, 0, output, 0, Math.min(serializedOutput.length, output.length));
    }

    public int getOutput(final Direction localDirection) {
        return output[localDirection.get3DDataValue()];
    }

    public void refreshInputs() {
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

    @Callback(synchronize = false,
        description = "Gets the received redstone signal for the specified side. The device's own output on that side counts towards the received signal.",
        returnValueDescription = "the current input signal strength.")
    public int getRedstoneInput(@Parameter(value = "side", description = SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        final int index = toLocalIndex(side);

        return input[index];
    }

    @Callback(synchronize = false,
        description = "Gets the emitted redstone signal for the specified side. This is the value last set via setRedstoneOutput().",
        returnValueDescription = "the current output signal strength.")
    public int getRedstoneOutput(@Parameter(value = "side", description = SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        final int index = toLocalIndex(side);

        return output[index];
    }

    @Callback(description = "Sets the emitted redstone signal for the specified side.")
    public void setRedstoneOutput(@Parameter(value = "side", description = SIDE) @Nullable final Side side,
                                  @Parameter(value = "value", description = "the signal strength to set, in the range of [0, 15]. Values outside are clamped.") final int value) {
        if (side == null) throw new IllegalArgumentException();
        final int index = toLocalIndex(side);

        final byte clampedValue = (byte) Mth.clamp(value, 0, 15);
        if (clampedValue == output[index]) {
            return;
        }

        output[index] = clampedValue;

        final Direction direction = HorizontalBlockUtils.toGlobal(host.getBlockState(), side);
        if (direction != null) {
            notifyNeighbor(direction);
        }

        host.setChanged();
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_REDSTONE_INPUT_CODE, synchronize = false, name = "getRedstoneInput",
        description = "Reads the level received on that side.",
        argumentsDescription = SIDE_IO,
        resultsDescription = LEVEL_IO)
    public void getRedstoneInputIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(getRedstoneInput(Side.byIndex(arguments.readU8())));
    }

    @IOCallback(value = GET_REDSTONE_OUTPUT_CODE, synchronize = false, name = "getRedstoneOutput",
        description = "Reads the level currently being sent on that side.",
        argumentsDescription = SIDE_IO,
        resultsDescription = LEVEL_IO)
    public void getRedstoneOutputIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(getRedstoneOutput(Side.byIndex(arguments.readU8())));
    }

    @IOCallback(value = SET_REDSTONE_OUTPUT_CODE, name = "setRedstoneOutput",
        description = "Sets the level sent on that side.",
        argumentsDescription = "two bytes, the side and the level.")
    public void setRedstoneOutputIO(final IOInputStream arguments) throws IOException {
        final Side side = Side.byIndex(arguments.readU8());
        setRedstoneOutput(side, arguments.readU8());
    }

    // --------------------------------------------------------------------- //

    private int toLocalIndex(final Side side) {
        return HorizontalBlockUtils.toLocal(host.getBlockState(), side).get3DDataValue();
    }

    private int readInput(final Direction direction) {
        final Level level = host.getLevel();
        if (level == null) {
            return 0;
        }

        final BlockPos neighborPos = host.getBlockPos().relative(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return level.getSignal(neighborPos, direction);
    }

    private void updateInputs(final boolean sendEvents) {
        for (final Direction direction : Direction.values()) {
            final int index = HorizontalBlockUtils.toLocal(host.getBlockState(), direction).get3DDataValue();

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
        final Level level = host.getLevel();
        if (level == null) {
            return;
        }

        level.updateNeighborsAt(host.getBlockPos(), host.getBlockState().getBlock());
        level.updateNeighborsAt(host.getBlockPos().relative(direction), host.getBlockState().getBlock());
    }
}
