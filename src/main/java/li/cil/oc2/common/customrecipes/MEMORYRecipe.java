package li.cil.oc2.common.customrecipes;

import com.google.gson.JsonObject;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.MemoryItem;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;

public class MEMORYRecipe implements ICraftingRecipe {
    private static final RegistryObject<Item> regObject = RegistryObject.of(new ResourceLocation("oc2:memory"), ForgeRegistries.ITEMS);

    private final ResourceLocation id;

    public MEMORYRecipe(ResourceLocation idIn) {
        id = idIn;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull ItemStack getRecipeOutput() {
        return new ItemStack(regObject.get());
    }

    @Override
    public boolean matches(@NotNull CraftingInventory inv, @NotNull World worldIn) {
        int level = getLevelOfMemory(inv);
        return level == 2 || level == 4 || level == 8;
    }

    @Override
    public @NotNull ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
        int level = getLevelOfMemory(inv);

        return (level == 2 || level == 4 || level == 8) ? MemoryItem.withCapacity(level * Constants.MEGABYTE) : ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public @NotNull IRecipeSerializer<?> getSerializer() {
        return CustomRecipes.MEMORY_RECIPE.get();
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<MEMORYRecipe> {
        @Override
        public @NotNull MEMORYRecipe read(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            return new MEMORYRecipe(recipeId);
        }

        @Override
        public MEMORYRecipe read(@NotNull ResourceLocation recipeId, @NotNull PacketBuffer buffer) {
            return new MEMORYRecipe(recipeId);
        }

        @Override
        public void write(@NotNull PacketBuffer buffer, @NotNull MEMORYRecipe recipe) {

        }
    }

    private int getLevelOfMemory(CraftingInventory inv) {
        if(!inv.getStackInSlot(0).getItem().equals(Items.PCB_ITEM.get())) return 0;

        int value = 0;
        for (int i = 1; i < 9; i++) {
            if(inv.getStackInSlot(i).getItem().equals(Items.MICROCHIP_ITEM.get())) {
                value++;
            }
        }
        int valueEmpty = 0;
        for (int i = 1; i < 9; i++) {
            if(inv.getStackInSlot(i).isEmpty()) {
                valueEmpty++;
            }
        }
        if(valueEmpty + value == 8) {
            return value;
        }
        return 0;
    }
}