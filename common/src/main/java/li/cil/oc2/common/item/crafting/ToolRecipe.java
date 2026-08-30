/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.StorageItemUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public final class ToolRecipe extends ShapelessRecipe {
    public ToolRecipe(final ShapelessRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.result, recipe.getIngredients());
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        if (!super.matches(input, level)) {
            return false;
        }

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (StorageItemUtils.needsRepair(stack)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        final NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (stack.getItem().hasCraftingRemainingItem()) {
                result.set(slot, new ItemStack(stack.getItem().getCraftingRemainingItem()));
            } else if (isTool(stack)) {
                final ItemStack copy = stack.copy();
                copy.setCount(1);
                result.set(slot, copy);
            } else {
                // Eager cleanup of blob storage; we convert to something new, so
                // the old data is unreachable anyway.
                StorageItemUtils.clearBlobData(stack);
            }
        }

        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.TOOL.get();
    }

    // --------------------------------------------------------------------- //

    private static boolean isTool(final ItemStack stack) {
        return Wrenches.isWrench(stack) || stack.is(ItemTags.DEVICES_CPU);
    }

    // --------------------------------------------------------------------- //

    public static final class Serializer implements RecipeSerializer<ToolRecipe> {
        private static final MapCodec<ToolRecipe> CODEC = RecipeSerializer.SHAPELESS_RECIPE.codec()
            .xmap(ToolRecipe::new, Function.identity());
        private static final StreamCodec<RegistryFriendlyByteBuf, ToolRecipe> STREAM_CODEC = RecipeSerializer.SHAPELESS_RECIPE.streamCodec()
            .map(ToolRecipe::new, Function.identity());

        @Override
        public MapCodec<ToolRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
