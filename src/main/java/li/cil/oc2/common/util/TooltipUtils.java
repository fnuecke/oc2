package li.cil.oc2.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;

import static li.cil.oc2.Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME;
import static li.cil.oc2.Constants.BLOCK_ENTITY_TAG_NAME_IN_ITEM;

public final class TooltipUtils {
    public static void addInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        final CompoundNBT tileEntityNbt = stack.getChildTag(BLOCK_ENTITY_TAG_NAME_IN_ITEM);
        if (tileEntityNbt != null && tileEntityNbt.contains(BLOCK_ENTITY_INVENTORY_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            final CompoundNBT itemHandlerNbt = tileEntityNbt.getCompound(BLOCK_ENTITY_INVENTORY_TAG_NAME);
            final ListNBT itemsNbt = itemHandlerNbt.getList("Items", NBTTagIds.TAG_COMPOUND);
            for (int i = 0; i < itemsNbt.size(); i++) {
                final CompoundNBT itemNbt = itemsNbt.getCompound(i);
                final ItemStack itemStack = ItemStack.read(itemNbt);
                tooltip.add(new StringTextComponent(" - ").append(itemStack.getDisplayName()));
            }
        }
    }
}
