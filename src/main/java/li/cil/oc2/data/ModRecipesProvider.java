package li.cil.oc2.data;

import li.cil.oc2.common.item.Items;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;

public final class ModRecipesProvider extends RecipeProvider {
    public ModRecipesProvider(final DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder
                .shaped(Items.COMPUTER.get())
                .pattern("ICI")
                .pattern("XTX")
                .pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.CHESTS_WOODEN)
                .define('X', Items.BUS_INTERFACE.get())
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .unlockedBy("has_circuit_board", inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.BUS_CABLE.get(), 16)
                .pattern("III")
                .pattern("GTG")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);

        ShapelessRecipeBuilder
                .shapeless(Items.BUS_INTERFACE.get())
                .requires(Items.TRANSISTOR.get())
                .requires(Items.BUS_CABLE.get())
                .unlockedBy("has_bus_cable", inventoryChange(Items.BUS_CABLE.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.NETWORK_CONNECTOR.get(), 4)
                .pattern("IGI")
                .pattern("ITI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.GLASS)
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.NETWORK_HUB.get())
                .pattern("ICI")
                .pattern("XTX")
                .pattern("ICI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.NETWORK_CONNECTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy("has_network_connector", inventoryChange(Items.NETWORK_CONNECTOR.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.REDSTONE_INTERFACE.get())
                .pattern("IRI")
                .pattern("XTX")
                .pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('T', Items.TRANSISTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.DISK_DRIVE.get())
                .pattern("IBI")
                .pattern("XTX")
                .pattern("IDI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', ItemTags.BUTTONS)
                .define('T', Items.TRANSISTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .define('D', net.minecraft.world.item.Items.DISPENSER)
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.CHARGER.get())
                .pattern("IPI")
                .pattern("XTX")
                .pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', net.minecraft.world.item.Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                .define('T', Items.TRANSISTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);


        ShapedRecipeBuilder
                .shaped(Items.WRENCH.get())
                .pattern("I I")
                .pattern(" T ")
                .pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);

        WrenchRecipeBuilder
                .wrenchRecipe(Items.MANUAL.get())
                .requires(net.minecraft.world.item.Items.BOOK)
                .unlockedBy("has_book", inventoryChange(net.minecraft.world.item.Items.BOOK))
                .unlockedBy("has_wrench", inventoryChange(Items.WRENCH.get()))
                .save(consumer);


        ShapedRecipeBuilder
                .shaped(Items.NETWORK_CABLE.get(), 8)
                .pattern("SSS")
                .pattern("GTG")
                .pattern("SSS")
                .define('S', Tags.Items.STRING)
                .define('G', Tags.Items.GLASS)
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy("has_network_connector", inventoryChange(Items.NETWORK_CONNECTOR.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.ROBOT.get())
                .pattern("ICI")
                .pattern("PTP")
                .pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.CHESTS_WOODEN)
                .define('P', net.minecraft.world.item.Items.PISTON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .unlockedBy("has_circuit_board", inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.MEMORY_SMALL.get(), 2)
                .pattern("ITI")
                .pattern(" B ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.MEMORY_MEDIUM.get(), 2)
                .pattern("GTG")
                .pattern(" B ")
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.MEMORY_LARGE.get(), 2)
                .pattern("DTD")
                .pattern(" B ")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.HARD_DRIVE_SMALL.get())
                .pattern("ITI")
                .pattern("EBE")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .define('E', Tags.Items.GEMS_EMERALD)
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.HARD_DRIVE_MEDIUM.get())
                .pattern("GTG")
                .pattern("EBE")
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .define('E', Tags.Items.GEMS_EMERALD)
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.HARD_DRIVE_LARGE.get())
                .pattern("DTD")
                .pattern("EBE")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .define('E', Tags.Items.GEMS_EMERALD)
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        WrenchRecipeBuilder
                .wrenchRecipe(Items.HARD_DRIVE_CUSTOM.get())
                .requires(Items.HARD_DRIVE_LARGE.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.FLASH_MEMORY.get())
                .pattern("ITI")
                .pattern("RBR")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        WrenchRecipeBuilder
                .wrenchRecipe(Items.FLASH_MEMORY_CUSTOM.get())
                .requires(Items.FLASH_MEMORY.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.REDSTONE_INTERFACE_CARD.get())
                .pattern("IRT")
                .pattern(" B ")
                .define('R', net.minecraft.world.item.Items.REDSTONE_TORCH)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.NETWORK_INTERFACE_CARD.get())
                .pattern("IGT")
                .pattern(" B ")
                .define('G', Tags.Items.GLASS)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.FILE_IMPORT_EXPORT_CARD.get())
                .pattern("IET")
                .pattern(" B ")
                .define('E', net.minecraft.world.item.Items.PAPER)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.SOUND_CARD.get())
                .pattern("IST")
                .pattern(" B ")
                .define('S', net.minecraft.world.item.Items.NOTE_BLOCK)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.FLOPPY.get())
                .pattern("ITI")
                .pattern("QBQ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('Q', Tags.Items.GEMS_QUARTZ)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_disk_drive", inventoryChange(Items.DISK_DRIVE.get()))
                .save(consumer);


        ShapedRecipeBuilder
                .shaped(Items.INVENTORY_OPERATIONS_MODULE.get())
                .pattern("TCG")
                .pattern(" B ")
                .define('T', Items.TRANSISTOR.get())
                .define('C', Tags.Items.CHESTS_WOODEN)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(Items.BLOCK_OPERATIONS_MODULE.get())
                .pattern("TPG")
                .pattern(" B ")
                .define('T', Items.TRANSISTOR.get())
                .define('P', net.minecraft.world.item.Items.DIAMOND_PICKAXE)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
                .save(consumer);


        ShapedRecipeBuilder
                .shaped(Items.TRANSISTOR.get(), 8)
                .pattern("RCR")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', net.minecraft.world.item.Items.COMPARATOR)
                .unlockedBy("has_gold", inventoryChange(net.minecraft.world.item.Items.GOLD_INGOT))
                .save(consumer);

        ShapelessRecipeBuilder
                .shapeless(Items.CIRCUIT_BOARD.get(), 4)
                .requires(Tags.Items.INGOTS_GOLD)
                .requires(net.minecraft.world.item.Items.CLAY_BALL)
                .requires(Items.TRANSISTOR.get())
                .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);
    }

    private static InventoryChangeTrigger.TriggerInstance inventoryChange(final ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }
}
