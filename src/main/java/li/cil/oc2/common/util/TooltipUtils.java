package li.cil.oc2.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

public final class TooltipUtils {
    private static final ThreadLocal<List<ItemStack>> ITEM_STACKS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<IntList> ITEM_STACKS_SIZES = ThreadLocal.withInitial(IntArrayList::new);

    public static void addInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        addInventoryInformation(stack, tooltip, new String[0]);
    }

    public static void addInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip, final String... itemHandlerTags) {
        final CompoundNBT itemHandlerNbt = ItemStackUtils.getTileEntityInventoryTag(stack);
        if (itemHandlerNbt != null) {
            final List<ItemStack> itemStacks = ITEM_STACKS.get();
            itemStacks.clear();
            final IntList itemStackSizes = ITEM_STACKS_SIZES.get();
            itemStackSizes.clear();

            collectItemStacks(itemHandlerNbt, itemStacks, itemStackSizes);

            for (final String itemHandlerTagName : itemHandlerTags) {
                if (itemHandlerNbt.contains(itemHandlerTagName, NBTTagIds.TAG_COMPOUND)) {
                    collectItemStacks(itemHandlerNbt.getCompound(itemHandlerTagName), itemStacks, itemStackSizes);
                }
            }

            for (int i = 0; i < itemStacks.size(); i++) {
                final ItemStack itemStack = itemStacks.get(i);
                tooltip.add(new StringTextComponent("")
                        .append(itemStack.getDisplayName())
                        .modifyStyle(style -> style.setColor(Color.fromTextFormatting(TextFormatting.GRAY)))
                        .append(new StringTextComponent(" x")
                                .appendString(String.valueOf(itemStackSizes.getInt(i)))
                                .modifyStyle(style -> style.setColor(Color.fromTextFormatting(TextFormatting.DARK_GRAY))))
                );
            }
        }
    }

    private static void collectItemStacks(final CompoundNBT nbt, final List<ItemStack> stacks, final IntList stackSizes) {
        final ListNBT itemsNbt = nbt.getList("Items", NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < itemsNbt.size(); i++) {
            final CompoundNBT itemNbt = itemsNbt.getCompound(i);
            final ItemStack itemStack = ItemStack.read(itemNbt);

            boolean didMerge = false;
            for (int j = 0; j < stacks.size(); j++) {
                final ItemStack existingStack = stacks.get(j);
                if (ItemStack.areItemsEqual(existingStack, itemStack) &&
                    ItemStack.areItemStackTagsEqual(existingStack, itemStack)) {
                    final int existingCount = stackSizes.getInt(j);
                    stackSizes.set(j, existingCount + itemStack.getCount());
                    didMerge = true;
                    break;
                }
            }

            if (!didMerge) {
                stacks.add(itemStack);
                stackSizes.add(itemStack.getCount());
            }
        }
    }
}
