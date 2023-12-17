/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import com.google.gson.JsonObject;
import li.cil.oc2.common.integration.Wrenches;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

public final class WrenchRecipe extends ShapelessRecipe {
    public WrenchRecipe(final ShapelessRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.getResultItem(), recipe.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingContainer inventory) {
        final NonNullList<ItemStack> result = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack.hasCraftingRemainingItem()) {
                result.set(slot, stack.getCraftingRemainingItem());
            } else if (Wrenches.isWrench(stack)) {
                final ItemStack copy = stack.copy();
                copy.setCount(1);
                result.set(slot, copy);
            }
        }

        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements RecipeSerializer<WrenchRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public WrenchRecipe fromJson(final ResourceLocation location, final JsonObject json) {
            return new WrenchRecipe(SHAPELESS_RECIPE.fromJson(location, json));
        }

        @Nullable
        @Override
        public WrenchRecipe fromNetwork(final ResourceLocation location, final FriendlyByteBuf buffer) {
            final ShapelessRecipe recipe = SHAPELESS_RECIPE.fromNetwork(location, buffer);
            if (recipe == null) {
                return null;
            }

            return new WrenchRecipe(recipe);
        }

        @Override
        public void toNetwork(final FriendlyByteBuf buffer, final WrenchRecipe recipe) {
            SHAPELESS_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
