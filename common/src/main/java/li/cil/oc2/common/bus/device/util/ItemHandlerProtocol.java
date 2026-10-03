/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.common.bus.device.SystemDevice;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

public final class ItemHandlerProtocol {
    private static final int SLOT_RECORD_SIZE = 4;
    private static final int MAX_SLOT_RECORDS = IOCallback.MAX_DATA_SIZE / SLOT_RECORD_SIZE;

    // --------------------------------------------------------------------- //

    public static void writeSlotCount(final ItemHandler handler, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(handler.getSlots(), 0xFF));
    }

    public static void writeSlots(final ItemHandler handler, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int first = ItemHandlerDeviceUtils.requireValidSlot(handler, arguments.readU8());
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
        writeSlotAt(handler, ItemHandlerDeviceUtils.requireValidSlot(handler, slot), results);
    }

    public static void writeSlotLimit(final ItemHandler handler, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(handler.getSlotLimit(ItemHandlerDeviceUtils.requireValidSlot(handler, arguments.readU8())), 0xFF));
    }

    // --------------------------------------------------------------------- //

    private static void writeSlotAt(final ItemHandler handler, final int slot, final IOOutputStream results) throws IOException {
        final ItemStack stack = handler.getStackInSlot(slot);
        results.writeU16(SystemDevice.toId(BuiltInRegistries.ITEM, stack.getItem()));
        results.writeU8(Math.min(stack.getCount(), 0xFF));
        results.writeU8(toDamage(stack));
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
