/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;

import java.io.IOException;

@RPCDeviceDescription(typeNames = {"composter"}, description = """
    Provided by composters connected to a [bus interface](../block/bus_interface.md).""")
@IODeviceDescription(name = "CMPSTR")
public final class ComposterDevice extends AbstractBlockDevice {
    private static final int GET_LEVEL_CODE = 1;

    // --------------------------------------------------------------------- //

    public ComposterDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how full the composter is.",
        returnValueDescription = "the level, from `0` to `8`.")
    public int getLevel() {
        return getBlockState(Blocks.COMPOSTER).getValue(ComposterBlock.LEVEL);
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_LEVEL_CODE,
        description = "Gets how full the composter is.",
        resultsDescription = "one byte, the level, from `0` to `8`.")
    public void getLevel(final IOOutputStream results) throws IOException {
        results.writeU8(getLevel());
    }
}
