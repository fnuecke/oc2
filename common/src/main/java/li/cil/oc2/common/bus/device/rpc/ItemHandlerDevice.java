/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IODeviceDescription;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.ItemHandlerProtocol;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

@RPCDeviceDescription(typeNames = {"item_handler"}, description = """
    Provided by any inventory connected through a [bus interface](../block/bus_interface.md), such as a chest. To move items between inventories, use a [transposer](../block/transposer.md).

    With several inventories connected, `find` may return any of them. To grab a specific one, give the bus interface in front of the one you want a name with a [wrench](../item/wrench.md), and find it by that name instead.""")
@IODeviceDescription(name = "ITEMS", description = """
    Each inventory is listed separately. When several are connected, pass a count in `B` to `OCFIND` to pick one, or check what `DEVS` lists.

    Items are identified by ids to save bus bandwidth. An id is stable within a world, but not across changes to the installed mods. An empty slot reads as item 0. Item numbers are two bytes, low byte first.

    A slot the inventory does not have, or a name no item goes by, fails with `OCEARG`.""")
public final class ItemHandlerDevice extends IdentityProxy<ItemHandler> {
    private static final int GET_SLOT_COUNT_CODE = 1;
    private static final int GET_SLOTS_CODE = 2;
    private static final int GET_SLOT_LIMIT_CODE = 3;
    private static final int GET_ITEM_NAME_CODE = 4;
    private static final int GET_ITEM_ID_CODE = 5;

    private static final String SLOT = "the number of the slot to look at.";

    // --------------------------------------------------------------------- //

    public ItemHandlerDevice(final ItemHandler identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how many slots the inventory has.",
        returnValueDescription = "the number of slots.")
    public int getItemSlotCount() {
        return identity.getSlots();
    }

    @Callback(description = "Gets what is in the specified slot.",
        returnValueDescription = "a table with the item information. Returns nothing for an empty slot.")
    public ItemStack getItemStackInSlot(@Parameter(value = "slot", description = SLOT) final int slot) {
        return identity.getStackInSlot(ItemHandlerProtocol.requireValidSlot(identity, slot));
    }

    @Callback(description = "Gets how many items the specified slot can hold.",
        returnValueDescription = "the most items the slot takes.")
    public int getItemSlotLimit(@Parameter(value = "slot", description = SLOT) final int slot) {
        return identity.getSlotLimit(ItemHandlerProtocol.requireValidSlot(identity, slot));
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_SLOT_COUNT_CODE, name = "getSlotCount",
        description = "Reads how many slots the inventory has.",
        resultsDescription = "one byte, the slot count, at most 255.")
    public void getItemSlotCountIO(final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlotCount(identity, results);
    }

    @IOCallback(value = GET_SLOTS_CODE, name = "getSlots",
        description = """
            Reads a run of slots in one call.
            Ask for 1 to 64 slots; more than that fails with `OCEARG`, since the reply would not fit. Reading stops at the end of the inventory, so asking for 64 slots starting at 0 gives you as many as there are. Damage is in [0, 255], where 0 means undamaged, or an item that does not take damage at all, and 255 means about to break.""",
        argumentsDescription = "two bytes, the slot to start at and how many slots to read.",
        resultsDescription = "four bytes per slot: the item as two bytes, the number of items up to 255, and damage.")
    public void getItemSlotsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlots(identity, arguments, results);
    }

    @IOCallback(value = GET_SLOT_LIMIT_CODE, name = "getSlotLimit",
        description = "Reads how much the slot can hold.",
        argumentsDescription = "one byte, the slot.",
        resultsDescription = "one byte, the limit, at most 255.")
    public void getItemSlotLimitIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlotLimit(identity, arguments, results);
    }

    @IOCallback(value = GET_ITEM_NAME_CODE, synchronize = false, name = "getItemName",
        description = "Reads the name of an item.",
        argumentsDescription = "two bytes, the item id.",
        resultsDescription = "the name, such as `minecraft:redstone`. Read while `OCDAV` is set to get all of it.")
    public void getItemNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeItemName(arguments, results);
    }

    @IOCallback(value = GET_ITEM_ID_CODE, synchronize = false, name = "getItemId",
        description = "Looks an item up by name.",
        argumentsDescription = "the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.",
        resultsDescription = "two bytes, the item id.")
    public void getItemIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeItemId(arguments, results);
    }
}
