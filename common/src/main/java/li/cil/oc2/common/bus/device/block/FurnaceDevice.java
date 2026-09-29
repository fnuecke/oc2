/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.io.IOException;

@RPCDeviceDescription(typeNames = {"furnace"}, description = """
    Provided by furnaces, smokers and blast furnaces connected to a [bus interface](../block/bus_interface.md). Times are in ticks. Slots are available through the [item handler](item_handler.md) device.""")
@IODeviceDescription(name = "FRNACE", description = """
    Times are two bytes, low byte first, capped at 65535.""")
public final class FurnaceDevice extends AbstractBlockDevice {
    private static final int GET_BURN_TIME_CODE = 1;
    private static final int GET_BURN_DURATION_CODE = 2;
    private static final int GET_COOK_TIME_CODE = 3;
    private static final int GET_COOK_TIME_TOTAL_CODE = 4;
    private static final int IS_BURNING_CODE = 5;

    // --------------------------------------------------------------------- //

    public FurnaceDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how much longer the current fuel burns.",
        returnValueDescription = "the remaining burn time.")
    public int getBurnTime() {
        return getFurnace().litTime;
    }

    @Callback(description = "Gets how long the current fuel burns in total.",
        returnValueDescription = "the burn time of the last consumed fuel.")
    public int getBurnDuration() {
        return getFurnace().litDuration;
    }

    @Callback(description = "Gets how long the current item has been cooking.",
        returnValueDescription = "the cooking progress.")
    public int getCookTime() {
        return getFurnace().cookingProgress;
    }

    @Callback(description = "Gets how long the current item needs to cook.",
        returnValueDescription = "the time the item needs.")
    public int getCookTimeTotal() {
        return getFurnace().cookingTotalTime;
    }

    @Callback(description = "Gets whether the furnace is burning fuel.",
        returnValueDescription = "whether fuel is currently burning.")
    public boolean isBurning() {
        return getFurnace().litTime > 0;
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_BURN_TIME_CODE,
        description = "Gets how much longer the current fuel burns.",
        resultsDescription = "two bytes, the remaining burn time.")
    public void getBurnTime(final IOOutputStream results) throws IOException {
        results.writeU16(Math.min(getBurnTime(), 0xFFFF));
    }

    @IOCallback(value = GET_BURN_DURATION_CODE,
        description = "Gets how long the current fuel burns in total.",
        resultsDescription = "two bytes, the burn time of the last consumed fuel.")
    public void getBurnDuration(final IOOutputStream results) throws IOException {
        results.writeU16(Math.min(getBurnDuration(), 0xFFFF));
    }

    @IOCallback(value = GET_COOK_TIME_CODE,
        description = "Gets how long the current item has been cooking.",
        resultsDescription = "two bytes, the cooking progress.")
    public void getCookTime(final IOOutputStream results) throws IOException {
        results.writeU16(Math.min(getCookTime(), 0xFFFF));
    }

    @IOCallback(value = GET_COOK_TIME_TOTAL_CODE,
        description = "Gets how long the current item needs to cook.",
        resultsDescription = "two bytes, the time the item needs.")
    public void getCookTimeTotal(final IOOutputStream results) throws IOException {
        results.writeU16(Math.min(getCookTimeTotal(), 0xFFFF));
    }

    @IOCallback(value = IS_BURNING_CODE,
        description = "Gets whether the furnace is burning fuel.",
        resultsDescription = "one byte, `1` while fuel is currently burning, `0` otherwise.")
    public void isBurning(final IOOutputStream results) throws IOException {
        results.writeU8(isBurning() ? 1 : 0);
    }

    // --------------------------------------------------------------------- //

    private AbstractFurnaceBlockEntity getFurnace() {
        return getBlockEntity(AbstractFurnaceBlockEntity.class);
    }
}
