package li.cil.oc2.data;

import li.cil.oc2.common.item.Items;
import net.minecraft.data.*;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Consumer;

public final class CraftingRecipes extends RecipeProvider {

    public CraftingRecipes(final DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(final Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder
                .shapedRecipe(Items.WRENCH_ITEM.get())
                .patternLine("I I")
                .patternLine(" C ")
                .patternLine(" I ")
                .key('I', net.minecraft.item.Items.IRON_INGOT)
                .key('C', Items.MICROCHIP_ITEM.get())
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.TRANSISTOR_ITEM.get(), 4)
                .patternLine("III")
                .patternLine("GPG")
                .patternLine(" R ")
                .key('I', net.minecraft.item.Items.IRON_INGOT)
                .key('G', net.minecraft.item.Items.GOLD_NUGGET)
                .key('R', net.minecraft.item.Items.REDSTONE)
                .key('P', net.minecraft.item.Items.PAPER)
                .build(consumer);

        createInterfaceCard(Items.REDSTONE_INTERFACE_CARD_ITEM, net.minecraft.item.Items.REDSTONE_TORCH, consumer);

        createInterfaceCardBlock(Items.REDSTONE_INTERFACE_ITEM, Items.REDSTONE_INTERFACE_CARD_ITEM, consumer);

        ShapelessRecipeBuilder
                .shapelessRecipe(Items.PCB_ITEM.get())
                .addIngredient(net.minecraft.item.Items.GOLD_NUGGET)
                .addIngredient(Ingredient.fromItems(net.minecraft.item.Items.SLIME_BALL, net.minecraft.item.Items.HONEY_BOTTLE))
                .addIngredient(Ingredient.fromItems(net.minecraft.item.Items.KELP, net.minecraft.item.Items.GREEN_DYE))
                .build(consumer);

        createInterfaceCard(Items.NETWORK_INTERFACE_CARD_ITEM, Items.NETWORK_CABLE_ITEM.get(), consumer);

        createInterfaceCardBlock(Items.NETWORK_HUB_ITEM, Items.NETWORK_INTERFACE_CARD_ITEM, consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.NETWORK_CONNECTOR_ITEM.get(), 2)
                .patternLine("NWN")
                .patternLine("NWN")
                .key('N', net.minecraft.item.Items.GOLD_NUGGET)
                .key('W', Items.NETWORK_CABLE_ITEM.get())
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.NETWORK_CONNECTOR_ITEM.get(), 6)
                .patternLine("SSS")
                .patternLine("GGG")
                .key('G', net.minecraft.item.Items.GOLD_NUGGET)
                .key('S', net.minecraft.item.Items.STRING)
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.MICROCHIP_ITEM.get())
                .patternLine("III")
                .patternLine("RTR")
                .patternLine("III")
                .key('I', net.minecraft.item.Items.GOLD_NUGGET)
                .key('T', Items.TRANSISTOR_ITEM.get())
                .key('R', net.minecraft.item.Items.REDSTONE)
                .build(consumer);

        // MEMORY
        // HARD_DRIVE
        // FLASH_MEMORY_LINUX
        // FLASH_MEMORY

        ShapedRecipeBuilder
                .shapedRecipe(Items.DISK_PLATTER_ITEM.get(), 6)
                .patternLine(" N ")
                .patternLine("N N")
                .patternLine(" N ")
                .key('N', net.minecraft.item.Items.IRON_NUGGET)
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.CONTROL_UNIT_ITEM.get())
                .patternLine("IRI")
                .patternLine("TCT")
                .patternLine("ITI")
                .key('I', net.minecraft.item.Items.GOLD_NUGGET)
                .key('T', Items.TRANSISTOR_ITEM.get())
                .key('C', net.minecraft.item.Items.CLOCK)
                .key('R', net.minecraft.item.Items.REDSTONE)
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.COMPUTER_ITEM.get())
                .patternLine("IMI")
                .patternLine("BCB")
                .patternLine("IPI")
                .key('I', net.minecraft.item.Items.GOLD_NUGGET)
                .key('C', Tags.Items.CHESTS_WOODEN)
                .key('M', Items.MICROCHIP_ITEM.get())
                .key('P', Items.PCB_ITEM.get())
                .key('B', net.minecraft.item.Items.IRON_BARS)
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.BUS_INTERFACE_ITEM.get())
                .patternLine("N  ")
                .patternLine("NC ")
                .patternLine("N  ")
                .key('N', net.minecraft.item.Items.GOLD_NUGGET)
                .key('C', Items.BUS_CABLE_ITEM.get())
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.BUS_CABLE_ITEM.get(), 3)
                .patternLine("NNN")
                .patternLine("WWW")
                .patternLine("NNN")
                .key('N', net.minecraft.item.Items.GOLD_NUGGET)
                .key('W', Items.NETWORK_CABLE_ITEM.get())
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.CONTROL_UNIT_ITEM.get())
                .patternLine("IRI")
                .patternLine("TNT")
                .patternLine("ITI")
                .key('I', net.minecraft.item.Items.IRON_NUGGET)
                .key('T', Items.TRANSISTOR_ITEM.get())
                .key('N', Items.MICROCHIP_ITEM.get())
                .key('R', net.minecraft.item.Items.REDSTONE)
                .build(consumer);
    }

    private void createInterfaceCard(final RegistryObject<Item> result, final Item type, final Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder
                .shapedRecipe(result.get())
                .patternLine("POG")
                .patternLine("PC ")
                .patternLine("PI ")
                .key('I', net.minecraft.item.Items.GOLD_NUGGET)
                .key('P', net.minecraft.item.Items.IRON_NUGGET)
                .key('C', Items.PCB_ITEM.get())
                .key('O', Items.MICROCHIP_ITEM.get())
                .key('G', type)
                .build(consumer);
    }

    private void createInterfaceCardBlock(final RegistryObject<Item> result, final RegistryObject<Item> type, final Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder
                .shapedRecipe(result.get())
                .patternLine("GGG")
                .patternLine("GCG")
                .patternLine("GGG")
                .key('G', net.minecraft.item.Items.GOLD_NUGGET)
                .key('C', type.get())
                .build(consumer);
    }

}
