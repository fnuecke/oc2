/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.integration.Wrenches;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.function.Function;

public final class WrenchRecipe extends ShapelessRecipe {
    public WrenchRecipe(final ShapelessRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.result, recipe.getIngredients());
    }

    // ------------------------------------------------------------- //

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        final NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (stack.getItem().hasCraftingRemainingItem()) {
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
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    // ------------------------------------------------------------- //

    public static final class Serializer implements RecipeSerializer<WrenchRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<WrenchRecipe> CODEC = RecipeSerializer.SHAPELESS_RECIPE.codec()
                .xmap(WrenchRecipe::new, Function.identity());
        private static final StreamCodec<RegistryFriendlyByteBuf, WrenchRecipe> STREAM_CODEC = RecipeSerializer.SHAPELESS_RECIPE.streamCodec()
                .map(WrenchRecipe::new, Function.identity());

        @Override
        public MapCodec<WrenchRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WrenchRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
