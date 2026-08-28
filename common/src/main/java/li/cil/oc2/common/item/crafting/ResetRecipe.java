/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.oc2.common.util.StorageItemUtils.State;
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
        int damagedCount = 0, wrenchCount = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (StorageItemUtils.needsRepair(stack)) {
                damagedCount++;
            } else if (Wrenches.isWrench(stack)) {
                wrenchCount++;
            } else {
                return false;
            }
        }

        return damagedCount == 1 && wrenchCount == 1;
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        final ItemStack stack = findDamagedDataItem(input);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final ItemStack result = stack.copy();
        result.setCount(1);

        if (StorageItemUtils.getState(stack) == State.INCONSISTENT) {
            StorageItemUtils.setState(result, State.ACKNOWLEDGED);
        } else {
            StorageItemUtils.stripBlobData(result);
        }

        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        final NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (StorageItemUtils.getState(stack) == State.CORRUPTED) {
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

    private static ItemStack findDamagedDataItem(final CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && StorageItemUtils.needsRepair(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
