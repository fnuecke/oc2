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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
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

    private static final int SLOT_RECORD_SIZE = 4;
    private static final int MAX_SLOT_RECORDS = IOCallback.MAX_DATA_SIZE / SLOT_RECORD_SIZE;

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
        return identity.getStackInSlot(requireValidSlot(slot));
    }

    @Callback
    public int getItemSlotLimit(@Parameter("slot") final int slot) {
        return identity.getSlotLimit(requireValidSlot(slot));
    }

    // --------------------------------------------------------------------- //

    @IOCallback(GET_SLOT_COUNT_CODE)
    public void getItemSlotCountIO(final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getItemSlotCount(), 0xFF));
    }

    @IOCallback(GET_SLOTS_CODE)
    public void getItemSlotsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int first = requireValidSlot(arguments.readU8());
        final int requested = arguments.readU8();
        if (requested == 0 || requested > MAX_SLOT_RECORDS) {
            throw new IllegalArgumentException("slot count out of range: " + requested
                + " (expected 1 to " + MAX_SLOT_RECORDS + ")");
        }

        final int count = Math.min(requested, identity.getSlots() - first);
        for (int slot = first; slot < first + count; slot++) {
            final ItemStack stack = identity.getStackInSlot(slot);
            results.writeU16(toItemId(stack.getItem()));
            results.writeU8(Math.min(stack.getCount(), 0xFF));
            results.writeU8(toDamage(stack));
        }
    }

    @IOCallback(GET_SLOT_LIMIT_CODE)
    public void getItemSlotLimitIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getItemSlotLimit(arguments.readU8()), 0xFF));
    }

    @IOCallback(value = GET_ITEM_NAME_CODE, synchronize = false)
    public void getItemNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int id = arguments.readU16();
        // Not byId(): the item registry is defaulted, so that answers air for anything out of range.
        final ResourceLocation key = BuiltInRegistries.ITEM.getHolder(id)
            .orElseThrow(() -> new IllegalArgumentException("no item with id: " + id))
            .key().location();
        results.writeString(key.toString());
    }

    @IOCallback(value = GET_ITEM_ID_CODE, synchronize = false)
    public void getItemIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final String name = arguments.readString();
        final ResourceLocation key = ResourceLocation.tryParse(name);
        final Item item = key != null ? BuiltInRegistries.ITEM.getOptional(key).orElse(null) : null;
        if (item == null) {
            throw new IllegalArgumentException("no such item: " + name);
        }

        results.writeU16(toItemId(item));
    }

    // --------------------------------------------------------------------- //

    private int requireValidSlot(final int slot) {
        if (slot < 0 || slot >= identity.getSlots()) {
            throw new IllegalArgumentException("slot out of range: " + slot
                + " (expected 0 to " + (identity.getSlots() - 1) + ")");
        }
        return slot;
    }

    private static int toItemId(final Item item) {
        final int id = BuiltInRegistries.ITEM.getId(item);
        if (id > 0xFFFF) {
            throw new IllegalStateException("item id does not fit the guest protocol: " + id);
        }
        return id;
    }

    private static int toDamage(final ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 0;
        }

        final int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0;
        }

        return Mth.clamp((int) Math.ceil(stack.getDamageValue() * (double) 0xFF / maxDamage), 0, 0xFF);
    }
}
