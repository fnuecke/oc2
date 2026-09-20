/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.inventory.ItemHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

public final class ItemHandlerProtocol {
    private static final int SLOT_RECORD_SIZE = 4;
    private static final int MAX_SLOT_RECORDS = IOCallback.MAX_DATA_SIZE / SLOT_RECORD_SIZE;

    // --------------------------------------------------------------------- //

    public static int requireValidSlot(final ItemHandler handler, final int slot) {
        if (slot < 0 || slot >= handler.getSlots()) {
            throw new IllegalArgumentException("slot out of range: " + slot
                + " (expected 0 to " + (handler.getSlots() - 1) + ")");
        }
        return slot;
    }

    public static void writeSlotCount(final ItemHandler handler, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(handler.getSlots(), 0xFF));
    }

    public static void writeSlots(final ItemHandler handler, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int first = requireValidSlot(handler, arguments.readU8());
        final int requested = arguments.readU8();
        if (requested == 0 || requested > MAX_SLOT_RECORDS) {
            throw new IllegalArgumentException("slot count out of range: " + requested
                + " (expected 1 to " + MAX_SLOT_RECORDS + ")");
        }

        final int count = Math.min(requested, handler.getSlots() - first);
        for (int slot = first; slot < first + count; slot++) {
            writeSlotAt(handler, slot, results);
        }
    }

    public static void writeSlot(final ItemHandler handler, final int slot, final IOOutputStream results) throws IOException {
        writeSlotAt(handler, requireValidSlot(handler, slot), results);
    }

    public static void writeSlotLimit(final ItemHandler handler, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(handler.getSlotLimit(requireValidSlot(handler, arguments.readU8())), 0xFF));
    }

    public static void writeItemName(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int id = arguments.readU16();
        final ResourceLocation key = BuiltInRegistries.ITEM.getHolder(id)
            .orElseThrow(() -> new IllegalArgumentException("no item with id: " + id))
            .key().location();
        results.writeString(key.toString());
    }

    public static void writeItemId(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final String name = arguments.readString();
        final ResourceLocation key = ResourceLocation.tryParse(name);
        final Item item = key != null ? BuiltInRegistries.ITEM.getOptional(key).orElse(null) : null;
        if (item == null) {
            throw new IllegalArgumentException("no such item: " + name);
        }

        results.writeU16(toItemId(item));
    }

    // --------------------------------------------------------------------- //

    private static void writeSlotAt(final ItemHandler handler, final int slot, final IOOutputStream results) throws IOException {
        final ItemStack stack = handler.getStackInSlot(slot);
        results.writeU16(toItemId(stack.getItem()));
        results.writeU8(Math.min(stack.getCount(), 0xFF));
        results.writeU8(toDamage(stack));
    }

    private static int toItemId(final Item item) throws IOException {
        final int id = BuiltInRegistries.ITEM.getId(item);
        if (id > 0xFFFF) {
            throw new IOException("item id does not fit the guest protocol: " + id);
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

    private ItemHandlerProtocol() {
    }
}
