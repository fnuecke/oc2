package li.cil.oc2.common.customrecipes;

import com.google.gson.JsonObject;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.item.Items;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;

public class HDDRecipe implements ICraftingRecipe {
    private static final RegistryObject<Item> WRITTEN_BOOK = RegistryObject.of(new ResourceLocation("oc2:hard_drive"), ForgeRegistries.ITEMS);

    private final ResourceLocation id;

    public HDDRecipe(ResourceLocation idIn) {
        id = idIn;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull ItemStack getRecipeOutput() {
        return new ItemStack(WRITTEN_BOOK.get());
    }

    @Override
    public boolean matches(@NotNull CraftingInventory inv, @NotNull World worldIn) {
        return craftingTemplateForTiers(inv) && creatingTiers(inv) != -1;
    }

    @Override
    public @NotNull ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
        boolean checkTemplate = craftingTemplateForTiers(inv);

        if(!checkTemplate) return ItemStack.EMPTY;

        int tier = creatingTiers(inv);
        return (tier != -1) ? HardDriveItem.withCapacity(((int)Math.pow(2,tier)) * Constants.MEGABYTE) : ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public @NotNull IRecipeSerializer<?> getSerializer() {
        return CustomRecipes.HDD_RECIPE.get();
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<HDDRecipe> {
        @Override
        public @NotNull HDDRecipe read(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            return new HDDRecipe(recipeId);
        }

        @Override
        public HDDRecipe read(@NotNull ResourceLocation recipeId, @NotNull PacketBuffer buffer) {
            return new HDDRecipe(recipeId);
        }

        @Override
        public void write(@NotNull PacketBuffer buffer, @NotNull HDDRecipe recipe) {

        }
    }
    private int creatingTiers(CraftingInventory inv) {
        Item iron = net.minecraft.item.Items.IRON_INGOT;
        Item gold = net.minecraft.item.Items.GOLD_INGOT;
        Item diamond = net.minecraft.item.Items.DIAMOND;
        Item first = inv.getStackInSlot(2).getItem();
        Item second = inv.getStackInSlot(8).getItem();
        boolean ironTemplate = first.equals(iron) && second.equals(iron);
        boolean goldTemplate = first.equals(gold) && second.equals(gold);
        boolean diamondTemplate = first.equals(diamond) && second.equals(diamond);

        if(ironTemplate) return 1;
        else if(goldTemplate) return 2;
        else if(diamondTemplate) return 3;
        else return -1;

    }

    private boolean craftingTemplateForTiers(CraftingInventory inv) {

        Item slot5 = inv.getStackInSlot(5).getItem();

        return inv.getStackInSlot(0).getItem().equals(Items.MICROCHIP_ITEM.get())
            && inv.getStackInSlot(1).getItem().equals(Items.DISK_PLATTER_ITEM.get())
            && inv.getStackInSlot(3).getItem().equals(Items.PCB_ITEM.get())
            && inv.getStackInSlot(4).getItem().equals(Items.DISK_PLATTER_ITEM.get())
            && (slot5.equals(net.minecraft.item.Items.PISTON) || slot5.equals(net.minecraft.item.Items.STICKY_PISTON))
            && inv.getStackInSlot(6).getItem().equals(Items.MICROCHIP_ITEM.get())
            && inv.getStackInSlot(7).getItem().equals(Items.DISK_PLATTER_ITEM.get());
    }
}