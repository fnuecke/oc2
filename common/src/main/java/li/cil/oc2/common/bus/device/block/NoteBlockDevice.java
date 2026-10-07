/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import javax.annotation.Nullable;
import java.io.IOException;

@RPCDeviceDescription(typeName = "note_block", description = """
    Provided by note blocks connected to a [bus interface](../block/bus_interface.md).

    The device changes what the note block is set to. It does not play it, use redstone signals for that.

    Instrument names include: `harp` or `bass`, `bell`, `chime`, `flute`, `guitar`, `pling`, `xylophone`, `bit`.

    Note that the instrument configuration is transient. Environmental changes will override it back to its natural configuration.""")
@IODeviceDescription(name = "NOTE")
public final class NoteBlockDevice extends AbstractBlockDevice {
    private static final int GET_NOTE_CODE = 1;
    private static final int SET_NOTE_CODE = 2;
    private static final int GET_INSTRUMENT_CODE = 3;
    private static final int SET_INSTRUMENT_CODE = 4;

    private static final int MIN_NOTE = 0;
    private static final int MAX_NOTE = 24;

    // --------------------------------------------------------------------- //

    public NoteBlockDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets the note the block is tuned to.",
        returnValueDescription = "a note from `0` to `24`.")
    public int getNote() {
        return getBlockState(Blocks.NOTE_BLOCK).getValue(NoteBlock.NOTE);
    }

    @Callback(description = "Tunes the block to a note.")
    public void setNote(@Parameter(value = "note", description = "the note to tune to, from `0` to `24`.") final int note) {
        if (note < MIN_NOTE || note > MAX_NOTE) {
            throw new IllegalArgumentException("note must be between " + MIN_NOTE + " and " + MAX_NOTE);
        }

        setBlockState(getBlockState(Blocks.NOTE_BLOCK).setValue(NoteBlock.NOTE, note));
    }

    @Callback(description = "Gets the name of the instrument the block plays.",
        returnValueDescription = "the instrument name.")
    public String getInstrument() {
        return getBlockState(Blocks.NOTE_BLOCK).getValue(NoteBlock.INSTRUMENT).getSerializedName();
    }

    @Callback(description = "Sets the instrument the block plays.")
    public void setInstrument(@Nullable @Parameter(value = "instrument", description = "the instrument name.") final String instrument) {
        if (instrument == null) {
            throw new IllegalArgumentException("instrument is required");
        }

        setBlockState(getBlockState(Blocks.NOTE_BLOCK).setValue(NoteBlock.INSTRUMENT, toInstrument(instrument)));
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_NOTE_CODE,
        description = "Gets the note the block is tuned to.",
        resultsDescription = "one byte, the note, from `0` to `24`.")
    public void getNote(final IOOutputStream results) throws IOException {
        results.writeU8(getNote());
    }

    @IOCallback(value = SET_NOTE_CODE,
        description = "Tunes the block to a note.",
        argumentsDescription = "one byte, the note, from `0` to `24`.")
    public void setNote(final IOInputStream arguments) throws IOException {
        setNote(arguments.readU8());
    }

    @IOCallback(value = GET_INSTRUMENT_CODE,
        description = "Gets the name of the instrument the block plays.",
        resultsDescription = "the instrument name. Read while `OCDAV` is set to read fully.")
    public void getInstrument(final IOOutputStream results) throws IOException {
        results.writeString(getInstrument());
    }

    @IOCallback(value = SET_INSTRUMENT_CODE,
        description = "Sets the instrument the block plays.",
        argumentsDescription = "the instrument name.")
    public void setInstrument(final IOInputStream arguments) throws IOException {
        setInstrument(arguments.readString());
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
}
