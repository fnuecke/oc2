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
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
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
                .patternLine("XTX")
                .patternLine("ICI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('C', Items.NETWORK_CONNECTOR.get())
                .key('X', Items.BUS_INTERFACE.get())
                .key('T', Items.TRANSISTOR.get())
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
                .patternLine("XTX")
                .patternLine("IDI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('B', ItemTags.BUTTONS)
                .key('T', Items.TRANSISTOR.get())
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
                .addCriterion("has_network_connector", inventoryChange(Items.NETWORK_CONNECTOR.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.ROBOT.get())
                .patternLine("ICI")
                .patternLine("PTP")
                .patternLine("IBI")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('C', Tags.Items.CHESTS_WOODEN)
                .key('P', net.minecraft.item.Items.PISTON)
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .addCriterion("has_circuit_board", inventoryChange(Items.CIRCUIT_BOARD.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.MEMORY_SMALL.get(), 2)
                .patternLine("ITI")
                .patternLine(" B ")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.MEMORY_MEDIUM.get(), 2)
                .patternLine("GTG")
                .patternLine(" B ")
                .key('G', Tags.Items.INGOTS_GOLD)
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.MEMORY_LARGE.get(), 2)
                .patternLine("DTD")
                .patternLine(" B ")
                .key('D', Tags.Items.GEMS_DIAMOND)
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.HARD_DRIVE_SMALL.get())
                .patternLine("ITI")
                .patternLine("EBE")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .key('E', Tags.Items.GEMS_EMERALD)
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.HARD_DRIVE_MEDIUM.get())
                .patternLine("GTG")
                .patternLine("EBE")
                .key('G', Tags.Items.INGOTS_GOLD)
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .key('E', Tags.Items.GEMS_EMERALD)
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.HARD_DRIVE_LARGE.get())
                .patternLine("DTD")
                .patternLine("EBE")
                .key('D', Tags.Items.GEMS_DIAMOND)
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .key('E', Tags.Items.GEMS_EMERALD)
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        WrenchRecipeBuilder
                .wrenchRecipe(Items.HARD_DRIVE_CUSTOM.get())
                .addIngredient(Items.HARD_DRIVE_LARGE.get())
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.FLASH_MEMORY.get())
                .patternLine("ITI")
                .patternLine("RBR")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('T', Items.TRANSISTOR.get())
                .key('R', Tags.Items.DUSTS_REDSTONE)
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        WrenchRecipeBuilder
                .wrenchRecipe(Items.FLASH_MEMORY_CUSTOM.get())
                .addIngredient(Items.FLASH_MEMORY.get())
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.REDSTONE_INTERFACE_CARD.get())
                .patternLine("IRT")
                .patternLine(" B ")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('B', Items.CIRCUIT_BOARD.get())
                .key('T', Items.TRANSISTOR.get())
                .key('R', net.minecraft.item.Items.REDSTONE_TORCH)
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.NETWORK_INTERFACE_CARD.get())
                .patternLine("IGT")
                .patternLine(" B ")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('G', Tags.Items.GLASS)
                .key('T', Items.TRANSISTOR.get())
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_computer", inventoryChange(Items.COMPUTER.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.FLOPPY.get())
                .patternLine("ITI")
                .patternLine("QBQ")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('T', Items.TRANSISTOR.get())
                .key('Q', Tags.Items.GEMS_QUARTZ)
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_disk_drive", inventoryChange(Items.DISK_DRIVE.get()))
                .build(consumer);


        ShapedRecipeBuilder
                .shapedRecipe(Items.INVENTORY_OPERATIONS_MODULE.get())
                .patternLine("TCG")
                .patternLine(" B ")
                .key('T', Items.TRANSISTOR.get())
                .key('C', Tags.Items.CHESTS_WOODEN)
                .key('G', Tags.Items.INGOTS_GOLD)
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);

        ShapedRecipeBuilder
                .shapedRecipe(Items.BLOCK_OPERATIONS_MODULE.get())
                .patternLine("TPG")
                .patternLine(" B ")
                .key('T', Items.TRANSISTOR.get())
                .key('P', net.minecraft.item.Items.DIAMOND_PICKAXE)
                .key('G', Tags.Items.INGOTS_GOLD)
                .key('B', Items.CIRCUIT_BOARD.get())
                .addCriterion("has_robot", inventoryChange(Items.ROBOT.get()))
                .build(consumer);


        ShapedRecipeBuilder
                .shapedRecipe(Items.TRANSISTOR.get(), 8)
                .patternLine("RCR")
                .patternLine("III")
                .key('I', Tags.Items.INGOTS_IRON)
                .key('R', Tags.Items.DUSTS_REDSTONE)
                .key('C', net.minecraft.item.Items.COMPARATOR)
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
