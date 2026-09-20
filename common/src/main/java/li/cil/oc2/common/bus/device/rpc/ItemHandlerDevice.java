/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOName;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.ItemHandlerProtocol;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@IOName("ITEMS")
public final class ItemHandlerDevice extends IdentityProxy<ItemHandler> implements NamedDevice {
    private static final int GET_SLOT_COUNT_CODE = 1;
    private static final int GET_SLOTS_CODE = 2;
    private static final int GET_SLOT_LIMIT_CODE = 3;
    private static final int GET_ITEM_NAME_CODE = 4;
    private static final int GET_ITEM_ID_CODE = 5;

    // --------------------------------------------------------------------- //

    public ItemHandlerDevice(final ItemHandler identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singleton("item_handler");
    }

    @Callback
    public int getItemSlotCount() {
        return identity.getSlots();
    }

    @Callback
    public ItemStack getItemStackInSlot(@Parameter("slot") final int slot) {
        return identity.getStackInSlot(ItemHandlerProtocol.requireValidSlot(identity, slot));
    }

    @Callback
    public int getItemSlotLimit(@Parameter("slot") final int slot) {
        return identity.getSlotLimit(ItemHandlerProtocol.requireValidSlot(identity, slot));
    }

    // --------------------------------------------------------------------- //

    @IOCallback(GET_SLOT_COUNT_CODE)
    public void getItemSlotCountIO(final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlotCount(identity, results);
    }

    @IOCallback(GET_SLOTS_CODE)
    public void getItemSlotsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlots(identity, arguments, results);
    }

    @IOCallback(GET_SLOT_LIMIT_CODE)
    public void getItemSlotLimitIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlotLimit(identity, arguments, results);
    }

    @IOCallback(value = GET_ITEM_NAME_CODE, synchronize = false)
    public void getItemNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeItemName(arguments, results);
    }

    @IOCallback(value = GET_ITEM_ID_CODE, synchronize = false)
    public void getItemIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeItemId(arguments, results);
    }
}
