/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.util.StorageItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ResetRecipe extends CustomRecipe {
    public ResetRecipe(final CraftingBookCategory category) {
        super(category);
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        int corruptedCount = 0, wrenchCount = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (StorageItemUtils.isCorrupted(stack)) {
                corruptedCount++;
            } else if (Wrenches.isWrench(stack)) {
                wrenchCount++;
            } else {
                return false;
            }
        }

        return corruptedCount == 1 && wrenchCount == 1;
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        final ItemStack stack = findCorruptedDataItem(input);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final ItemStack result = stack.copy();
        result.setCount(1);
        StorageItemUtils.stripBlobData(result);

        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        final NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (StorageItemUtils.isCorrupted(stack)) {
                // Only reached when the crafting result is actually picked up.
                StorageItemUtils.clearBlobData(stack);
            } else if (stack.getItem().hasCraftingRemainingItem()) {
                result.set(slot, new ItemStack(stack.getItem().getCraftingRemainingItem()));
            } else if (Wrenches.isWrench(stack)) {
                final ItemStack copy = stack.copy();
                copy.setCount(1);
                result.set(slot, copy);
            }
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.RESET.get();
    }

    // --------------------------------------------------------------------- //

    private static ItemStack findCorruptedDataItem(final CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && StorageItemUtils.isCorrupted(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
