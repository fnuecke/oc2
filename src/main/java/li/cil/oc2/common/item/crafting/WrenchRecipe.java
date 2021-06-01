package li.cil.oc2.common.item.crafting;

import com.google.gson.JsonObject;
import li.cil.oc2.common.integration.Wrenches;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

public final class WrenchRecipe extends ShapelessRecipe {
    public WrenchRecipe(final ShapelessRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.getResultItem(), recipe.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInventory inventory) {
        final NonNullList<ItemStack> result = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack.hasContainerItem()) {
                result.set(slot, stack.getContainerItem());
            } else if (Wrenches.isWrench(stack)) {
                final ItemStack copy = stack.copy();
                copy.setCount(1);
                result.set(slot, copy);
            }
        }

        return result;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return new Serializer();
    }

    public static final class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<WrenchRecipe> {
        @Override
        public WrenchRecipe fromJson(final ResourceLocation location, final JsonObject json) {
            return new WrenchRecipe(SHAPELESS_RECIPE.fromJson(location, json));
        }

        @Nullable
        @Override
        public WrenchRecipe fromNetwork(final ResourceLocation location, final PacketBuffer buffer) {
            return new WrenchRecipe(SHAPELESS_RECIPE.fromNetwork(location, buffer));
        }

        @Override
        public void toNetwork(final PacketBuffer buffer, final WrenchRecipe recipe) {
            SHAPELESS_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
