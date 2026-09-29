/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.block;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

import java.io.IOException;

@RPCDeviceDescription(typeNames = {"beehive"}, description = """
    Provided by beehives and bee nests connected to a [bus interface](../block/bus_interface.md).""")
@IODeviceDescription(name = "BHIVE")
public final class BeehiveDevice extends AbstractBlockDevice {
    private static final int GET_HONEY_LEVEL_CODE = 1;
    private static final int GET_BEE_COUNT_CODE = 2;
    private static final int IS_SEDATED_CODE = 3;

    // --------------------------------------------------------------------- //

    public BeehiveDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how much honey the hive holds.",
        returnValueDescription = "the honey level, from `0` to `5`.")
    public int getHoneyLevel() {
        return BeehiveBlockEntity.getHoneyLevel(getHive().getBlockState());
    }

    @Callback(description = "Gets how many bees are inside the hive.",
        returnValueDescription = "the number of bees.")
    public int getBeeCount() {
        return getHive().getOccupantCount();
    }

    @Callback(description = "Gets whether bees are currently sedated.",
        returnValueDescription = "`true` while the bees are calm.")
    public boolean isSedated() {
        return getHive().isSedated();
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_HONEY_LEVEL_CODE,
        description = "Gets how much honey the hive holds.",
        resultsDescription = "one byte, the honey level, from `0` to `5`.")
    public void getHoneyLevel(final IOOutputStream results) throws IOException {
        results.writeU8(getHoneyLevel());
    }

    @IOCallback(value = GET_BEE_COUNT_CODE,
        description = "Gets how many bees are inside the hive.",
        resultsDescription = "one byte, the number of bees.")
    public void getBeeCount(final IOOutputStream results) throws IOException {
        results.writeU8(getBeeCount());
    }

    @IOCallback(value = IS_SEDATED_CODE,
        description = "Gets whether bees are currently sedated.",
        resultsDescription = "one byte, `1` while bees are sedated, `0` otherwise.")
    public void isSedated(final IOOutputStream results) throws IOException {
        results.writeU8(isSedated() ? 1 : 0);
    }

    // --------------------------------------------------------------------- //

    private BeehiveBlockEntity getHive() {
        return getBlockEntity(BeehiveBlockEntity.class);
    }
}
