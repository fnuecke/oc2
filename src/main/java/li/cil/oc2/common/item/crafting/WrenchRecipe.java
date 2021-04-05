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
        super(recipe.getId(), recipe.getGroup(), recipe.getRecipeOutput(), recipe.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInventory inventory) {
        final NonNullList<ItemStack> result = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);

        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
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
        public WrenchRecipe read(final ResourceLocation location, final JsonObject json) {
            return new WrenchRecipe(CRAFTING_SHAPELESS.read(location, json));
        }

        @Nullable
        @Override
        public WrenchRecipe read(final ResourceLocation location, final PacketBuffer buffer) {
            return new WrenchRecipe(CRAFTING_SHAPELESS.read(location, buffer));
        }

        @Override
        public void write(final PacketBuffer buffer, final WrenchRecipe recipe) {
            CRAFTING_SHAPELESS.write(buffer, recipe);
        }
    }
}
