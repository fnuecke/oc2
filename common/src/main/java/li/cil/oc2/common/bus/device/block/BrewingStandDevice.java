/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.bus.device.rpc.block.AbstractBlockDevice;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import java.io.IOException;

@RPCDeviceDescription(typeNames = {"brewing_stand"}, description = """
    Provided by brewing stands connected to a [bus interface](../block/bus_interface.md). The stand's slots are available through the [item handler](item_handler.md) device.""")
@IODeviceDescription(name = "BREW")
public final class BrewingStandDevice extends AbstractBlockDevice {
    private static final int GET_BREW_TIME_CODE = 1;
    private static final int GET_FUEL_CODE = 2;

    // --------------------------------------------------------------------- //

    public BrewingStandDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how much longer the current brew takes, in ticks.",
        returnValueDescription = "the remaining time. `0` when not brewing.")
    public int getBrewTime() {
        return getBrewingStand().brewTime;
    }

    @Callback(description = "Gets how many more brews the stand's fuel lasts for.",
        returnValueDescription = "the remaining brews.")
    public int getFuel() {
        return getBrewingStand().fuel;
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_BREW_TIME_CODE,
        description = "Gets how much longer the current brew takes, in ticks.",
        resultsDescription = "two bytes, the time, low byte first. `0` when not brewing.")
    public void getBrewTime(final IOOutputStream results) throws IOException {
        results.writeU16(getBrewTime());
    }

    @IOCallback(value = GET_FUEL_CODE,
        description = "Gets how many more brews the stand's fuel lasts for.",
        resultsDescription = "one byte, the remaining brews.")
    public void getFuel(final IOOutputStream results) throws IOException {
        results.writeU8(getFuel());
    }

    // --------------------------------------------------------------------- //

    private BrewingStandBlockEntity getBrewingStand() {
        return getBlockEntity(BrewingStandBlockEntity.class);
    }
}
