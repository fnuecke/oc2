/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

import javax.annotation.Nullable;
import java.io.IOException;

@RPCDeviceDescription(typeNames = {"comparator"}, description = """
    Provided by redstone comparators connected to a [bus interface](../block/bus_interface.md).""")
@IODeviceDescription(name = "CMPRTR")
public final class ComparatorDevice extends AbstractBlockDevice {
    private static final int GET_OUTPUT_SIGNAL_CODE = 1;
    private static final int GET_MODE_CODE = 2;
    private static final int SET_MODE_CODE = 3;

    // --------------------------------------------------------------------- //

    public ComparatorDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets the signal strength the comparator outputs.",
        returnValueDescription = "the signal strength, from `0` to `15`.")
    public int getOutputSignal() {
        return getBlockEntity(ComparatorBlockEntity.class).getOutputSignal();
    }

    @Callback(description = "Gets the mode the comparator is in.",
        returnValueDescription = "`compare` or `subtract`.")
    public String getMode() {
        return getBlockState(Blocks.COMPARATOR).getValue(ComparatorBlock.MODE).getSerializedName();
    }

    @Callback(description = "Sets the mode the comparator is in.")
    public void setMode(@Nullable @Parameter(value = "mode", description = "`compare` or `subtract`.") final String mode) {
        for (final ComparatorMode value : ComparatorMode.values()) {
            if (value.getSerializedName().equals(mode)) {
                setMode(value);
                return;
            }
        }

        throw new IllegalArgumentException("mode must be compare or subtract");
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_OUTPUT_SIGNAL_CODE,
        description = "Gets the signal strength the comparator outputs.",
        resultsDescription = "one byte, the signal strength, from `0` to `15`.")
    public void getOutputSignal(final IOOutputStream results) throws IOException {
        results.writeU8(getOutputSignal());
    }

    @IOCallback(value = GET_MODE_CODE,
        description = "Gets the mode the comparator is in.",
        resultsDescription = "one byte, `0` is `compare`, `1` is `subtract`.")
    public void getMode(final IOOutputStream results) throws IOException {
        results.writeU8(getBlockState(Blocks.COMPARATOR).getValue(ComparatorBlock.MODE).ordinal());
    }

    @IOCallback(value = SET_MODE_CODE,
        description = "Sets the mode the comparator is in.",
        argumentsDescription = "one byte, `0` is `compare`, `1` is `subtract`.")
    public void setMode(final IOInputStream arguments) throws IOException {
        final int mode = arguments.readU8();
        if (mode >= ComparatorMode.values().length) {
            throw new IllegalArgumentException("mode must be 0 or 1");
        }

        setMode(ComparatorMode.values()[mode]);
    }

    // --------------------------------------------------------------------- //

    private void setMode(final ComparatorMode mode) {
        final BlockState blockState = getBlockState(Blocks.COMPARATOR);
        if (blockState.getValue(ComparatorBlock.MODE) == mode) {
            return;
        }

        setBlockState(blockState.setValue(ComparatorBlock.MODE, mode));
        getServerLevel().scheduleTick(identity.blockPos(), Blocks.COMPARATOR, 1);
    }
}
