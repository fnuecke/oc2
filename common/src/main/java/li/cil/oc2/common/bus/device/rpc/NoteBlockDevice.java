/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.util.BlockLocation;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import javax.annotation.Nullable;

@RPCDeviceDescription(typeNames = {"note_block"}, description = """
    Provided by note blocks connected to a [bus interface](../block/bus_interface.md).

    The device changes what the note block is set to. It does not play it, use redstone signals for that.

    Note that the instrument configuration is transient. Environmental changes will override it back to its natural configuration.""")
public final class NoteBlockDevice extends IdentityProxy<BlockLocation> {
    private static final int MIN_NOTE = 0;
    private static final int MAX_NOTE = 24;

    // --------------------------------------------------------------------- //

    public NoteBlockDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Returns the note the block is tuned to.",
        returnValueDescription = "a note from `0` to `24`.")
    public int getNote() {
        return getNoteBlockState().getValue(NoteBlock.NOTE);
    }

    @Callback(description = "Tunes the block to a note.")
    public void setNote(@Parameter(value = "note", description = "the note to tune to, from `0` to `24`.") final int note) {
        if (note < MIN_NOTE || note > MAX_NOTE) {
            throw new IllegalArgumentException("note must be between " + MIN_NOTE + " and " + MAX_NOTE);
        }

        setNoteBlockState(getNoteBlockState().setValue(NoteBlock.NOTE, note));
    }

    @Callback(description = "Returns the name of the instrument the block plays.",
        returnValueDescription = "the instrument name, such as `harp` or `bit`.")
    public String getInstrument() {
        return getNoteBlockState().getValue(NoteBlock.INSTRUMENT).getSerializedName();
    }

    @Callback(description = "Changes the instrument the block plays.")
    public void setInstrument(@Nullable @Parameter(value = "instrument", description = "the name of the instrument, such as `harp`, `bass`, `bell`, `chime`, `flute`, `guitar`, `pling`, `xylophone` or `bit`.") final String instrument) {
        if (instrument == null) {
            throw new IllegalArgumentException("instrument is required");
        }

        setNoteBlockState(getNoteBlockState().setValue(NoteBlock.INSTRUMENT, toInstrument(instrument)));
    }

    // --------------------------------------------------------------------- //

    private static NoteBlockInstrument toInstrument(final String name) {
        for (final NoteBlockInstrument instrument : NoteBlockInstrument.values()) {
            if (instrument.getSerializedName().equals(name)) {
                return instrument;
            }
        }

        throw new IllegalArgumentException("instrument not found");
    }

    private BlockState getNoteBlockState() {
        final BlockState blockState = getLevel().getBlockState(identity.blockPos());
        if (!blockState.is(Blocks.NOTE_BLOCK)) {
            throw new IllegalStateException("no note block at this position");
        }

        return blockState;
    }

    private void setNoteBlockState(final BlockState blockState) {
        final ServerLevel level = getLevel();
        if (!level.mayInteract(FakePlayerUtils.getFakePlayer(level), identity.blockPos())) {
            throw new IllegalStateException("not allowed");
        }

        level.setBlock(identity.blockPos(), blockState, Block.UPDATE_ALL);
    }

    private ServerLevel getLevel() {
        if (identity.tryGetLevel().orElse(null) instanceof final ServerLevel level) {
            return level;
        }

        throw new IllegalStateException("level is not loaded");
    }
}
