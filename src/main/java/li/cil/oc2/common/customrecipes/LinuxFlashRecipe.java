package li.cil.oc2.common.customrecipes;

import com.google.gson.JsonObject;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.item.FlashMemoryItem;
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

import java.util.Arrays;

public class LinuxFlashRecipe implements ICraftingRecipe {
    private static final RegistryObject<Item> regObject = RegistryObject.of(new ResourceLocation("oc2:flash_memory"), ForgeRegistries.ITEMS);

    private final ResourceLocation id;

    public LinuxFlashRecipe(ResourceLocation idIn) {
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
        return isValid(inv);
    }

    @Override
    public @NotNull ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
        return (isValid(inv)) ? FlashMemoryItem.withFirmware(Firmwares.BUILDROOT.get()) : ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public @NotNull IRecipeSerializer<?> getSerializer() {
        return CustomRecipes.FLASH_MEMORY_LINUX_RECIPE.get();
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<LinuxFlashRecipe> {
        @Override
        public @NotNull LinuxFlashRecipe read(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            return new LinuxFlashRecipe(recipeId);
        }

        @Override
        public LinuxFlashRecipe read(@NotNull ResourceLocation recipeId, @NotNull PacketBuffer buffer) {
            return new LinuxFlashRecipe(recipeId);
        }

        @Override
        public void write(@NotNull PacketBuffer buffer, @NotNull LinuxFlashRecipe recipe) {

        }
    }

    private boolean isValid(CraftingInventory inv) {
        ItemStack[] array = new ItemStack[8];

        for (int i = 0; i < 8; i++) {
            array[i] = inv.getStackInSlot(i);
        }

        boolean memoryCard = Arrays.stream(array).filter(x -> x.getItem().equals(Items.FLASH_MEMORY_ITEM.get())).count() == 1;
        boolean wrenchItem = Arrays.stream(array).filter(x -> x.getItem().equals(Items.WRENCH_ITEM.get())).count() == 1;
        boolean emptySlots = Arrays.stream(array).filter(ItemStack::isEmpty).count() == 6;

        return memoryCard && wrenchItem && emptySlots;
    }
}
