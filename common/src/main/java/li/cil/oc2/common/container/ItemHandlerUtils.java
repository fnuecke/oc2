/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public final class ItemHandlerUtils {
    public static final int ANY_SLOT = -1;

    // --------------------------------------------------------------------- //

    public static ItemStack insertItemStack(final ItemHandler handler, ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (stack.isStackable()) {
            for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
                final ItemStack existing = handler.getStackInSlot(slot);
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                    stack = handler.insertItem(slot, stack, simulate);
                }
            }

            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
            if (handler.getStackInSlot(slot).isEmpty()) {
                stack = handler.insertItem(slot, stack, simulate);
            }
        }

        return stack;
    }

    public static int transfer(final ItemHandler source, final int sourceSlot, final int count,
                               final BiFunction<ItemStack, Boolean, ItemStack> insert, final Consumer<ItemStack> overflow) {
        if (count <= 0) {
            return 0;
        }

        final ItemStack available = source.extractItem(sourceSlot, count, true);
        if (available.isEmpty()) {
            return 0;
        }

        final int accepted = available.getCount() - insert.apply(available, true).getCount();
        if (accepted <= 0) {
            return 0;
        }

        final ItemStack extracted = source.extractItem(sourceSlot, accepted, false);
        if (extracted.isEmpty()) {
            return 0;
        }

        final ItemStack remaining = insert.apply(extracted, false);
        if (!remaining.isEmpty()) {
            final ItemStack rejected = source.insertItem(sourceSlot, remaining, false);
            if (!rejected.isEmpty()) {
                overflow.accept(rejected);
            }
        }

        return extracted.getCount() - remaining.getCount();
    }

    public static int transferFirst(final List<ItemHandler> sources, final int sourceSlot,
                                    final List<ItemHandler> targets, final int targetSlot, final int targetStartSlot,
                                    final int count, final Consumer<ItemStack> overflow) {
        if (count <= 0) {
            return 0;
        }

        for (final ItemHandler source : sources) {
            for (final int from : sourceSlot == ANY_SLOT ? allSlots(source) : new int[]{sourceSlot}) {
                final ItemStack available = source.extractItem(from, count, true);
                if (available.isEmpty()) {
                    continue;
                }

                for (final ItemHandler target : targets) {
                    for (final int into : targetSlot == ANY_SLOT ? insertionOrder(target, available, targetStartSlot) : new int[]{targetSlot}) {
                        if (source == target && from == into) { //NOPMD - same handler instance
                            continue;
                        }

                        final int moved = transfer(source, from, count,
                            (stack, simulate) -> target.insertItem(into, stack, simulate), overflow);
                        if (moved > 0) {
                            return moved;
                        }
                    }
                }
            }
        }

        return 0;
    }

    // --------------------------------------------------------------------- //

    private static int[] allSlots(final ItemHandler handler) {
        return IntStream.range(0, handler.getSlots()).toArray();
    }

    private static int[] insertionOrder(final ItemHandler handler, final ItemStack stack, final int startSlot) {
        final int slots = handler.getSlots();
        final IntStream rotated = IntStream.range(0, slots).map(i -> Math.floorMod(startSlot + i, slots));
        final int[] order = rotated.toArray();
        return IntStream.concat(
            Arrays.stream(order).filter(slot -> ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), stack)),
            Arrays.stream(order).filter(slot -> handler.getStackInSlot(slot).isEmpty())
        ).toArray();
    }

    private ItemHandlerUtils() {
    }
}
