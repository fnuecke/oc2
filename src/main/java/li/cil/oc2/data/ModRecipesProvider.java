package li.cil.oc2.data;

import li.cil.oc2.common.item.Items;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.IItemProvider;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

public final class ModRecipesProvider extends RecipeProvider {

    public ModRecipesProvider(final DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(final Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder
                .shapedRecipe(Items.COMPUTER.get())
                .patternLine("ICI")
                .patternLine("XTX")
                .patternLine("IBI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('C', Tags.Items.CHESTS_WOODEN)
                .key('X', Items.BUS_INTERFACE.get())
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .addCriterion("has_circuit_board", inventoryChange(Items.CIRCUIT_BOARD.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.BUS_CABLE.get(), 16)
                .patternLine("III")
                .patternLine("GTG")
                .patternLine("III")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('G', Tags.Items.INGOTS_GOLD)
                .key('T', Items.TRANSISTOR.get())
                .addCriterion("has_gold", inventoryChange(net.minecraft.item.Items.GOLD_INGOT))
                .build(consumer);

        ShapelessRecipeBuilder
                .shapelessRecipe(Items.BUS_INTERFACE.get())
                .addIngredient(Items.TRANSISTOR.get())
                .addIngredient(Items.BUS_CABLE.get())
                .addCriterion("has_bus_cable", inventoryChange(Items.BUS_CABLE.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.NETWORK_CONNECTOR.get(), 4)
                .patternLine("IGI")
                .patternLine("ITI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('G', Tags.Items.GLASS)
                .key('T', Items.TRANSISTOR.get())
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.NETWORK_HUB.get())
                .patternLine("ICI")
                .patternLine("XRX")
                .patternLine("ICI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('C', Items.NETWORK_CONNECTOR.get())
                .key('R', net.minecraft.item.Items.REPEATER)
                .key('X', Items.BUS_INTERFACE.get())
                .addCriterion("has_network_connector", inventoryChange(Items.NETWORK_CONNECTOR.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.REDSTONE_INTERFACE.get())
                .patternLine("IRI")
                .patternLine("XTX")
                .patternLine("IRI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('R', Tags.Items.DUSTS_REDSTONE)
                .key('T', Items.TRANSISTOR.get())
                .key('X', Items.BUS_INTERFACE.get())
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.DISK_DRIVE.get())
                .patternLine("IBI")
                .patternLine("XSX")
                .patternLine("IDI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('B', ItemTags.BUTTONS)
                .key('S', net.minecraft.item.Items.STICK)
                .key('X', Items.BUS_INTERFACE.get())
                .key('D', net.minecraft.item.Items.DISPENSER)
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.CHARGER.get())
                .patternLine("IPI")
                .patternLine("XTX")
                .patternLine("IRI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('P', net.minecraft.item.Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                .key('T', Items.TRANSISTOR.get())
                .key('X', Items.BUS_INTERFACE.get())
                .key('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .build(consumer);


        ShapedRecipeBuilder
                .shapedRecipe(Items.WRENCH.get())
                .patternLine("I I")
                .patternLine(" T ")
                .patternLine(" I ")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('T', Items.TRANSISTOR.get())
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .build(consumer);


        ShapedRecipeBuilder
                .shapedRecipe(Items.NETWORK_CABLE.get(), 8)
                .patternLine("SSS")
                .patternLine("GTG")
                .patternLine("SSS")
                .key('S', Tags.Items.STRING)
                .key('G', Tags.Items.GLASS)
                .key('T', Items.TRANSISTOR.get())
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .build(consumer);

        // todo Robot

        // todo Memory
        // todo HardDrive
        // todo HardDrive - Linux
        // todo FlashMemory
        // todo FlashMemory - Linux

        ShapedRecipeBuilder
                .shapedRecipe(Items.REDSTONE_INTERFACE_CARD.get())
                .patternLine("IRT")
                .patternLine(" B ")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('B', Items.CIRCUIT_BOARD.get())
                .key('T', Items.TRANSISTOR.get())
                .key('R', Tags.Items.DUSTS_REDSTONE)
                .addCriterion("has_board", inventoryChange(Items.CIRCUIT_BOARD.get()))
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.NETWORK_INTERFACE_CARD.get())
                .patternLine("IRT")
                .patternLine(" B ")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('B', Items.CIRCUIT_BOARD.get())
                .key('T', Items.TRANSISTOR.get())
                .key('R', net.minecraft.item.Items.REPEATER)
                .addCriterion("has_board", inventoryChange(Items.CIRCUIT_BOARD.get()))
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .build(consumer);

        // todo Floppy


        // todo InventoryOperationsModule
        // todo BlockOperationsModule

        ShapedRecipeBuilder
                .shapedRecipe(Items.TRANSISTOR.get(), 8)
                .patternLine(" G ")
                .patternLine("RPR")
                .patternLine("III")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('G', Tags.Items.INGOTS_GOLD)
                .key('R', Tags.Items.DUSTS_REDSTONE)
                .key('P', net.minecraft.item.Items.PAPER)
                .addCriterion("has_gold", inventoryChange(net.minecraft.item.Items.GOLD_INGOT))
                .build(consumer);

        ShapelessRecipeBuilder
                .shapelessRecipe(Items.CIRCUIT_BOARD.get(), 4)
                .addIngredient(Tags.Items.INGOTS_GOLD)
                .addIngredient(net.minecraft.item.Items.CLAY_BALL)
                .addIngredient(Items.TRANSISTOR.get())
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .build(consumer);

    }

    private static InventoryChangeTrigger.Instance inventoryChange(final IItemProvider item) {
        return InventoryChangeTrigger.Instance.forItems(item);
    }
}
