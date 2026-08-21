/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.ResetRecipe;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public final class ModRecipesProvider extends RecipeProvider {
    public ModRecipesProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(final RecipeOutput consumer) {
        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.COMPUTER.get())
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
            .shaped(RecipeCategory.MISC, Items.BUS_CABLE.get(), 16)
            .pattern("III")
            .pattern("GTG")
            .pattern("III")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('T', Items.TRANSISTOR.get())
            .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
            .save(consumer);

        ShapelessRecipeBuilder
            .shapeless(RecipeCategory.MISC, Items.BUS_INTERFACE.get())
            .requires(Items.TRANSISTOR.get())
            .requires(Items.BUS_CABLE.get())
            .unlockedBy("has_bus_cable", inventoryChange(Items.BUS_CABLE.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.NETWORK_CONNECTOR.get(), 4)
            .pattern("IGI")
            .pattern("ITI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('G', Tags.Items.GLASS_BLOCKS)
            .define('T', Items.TRANSISTOR.get())
            .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.NETWORK_HUB.get())
            .pattern("ICI")
            .pattern("XTX")
            .pattern("IBI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('C', Items.NETWORK_CONNECTOR.get())
            .define('X', Items.BUS_INTERFACE.get())
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_network_connector", inventoryChange(Items.NETWORK_CONNECTOR.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.REDSTONE_INTERFACE.get())
            .pattern("ICI")
            .pattern("XTX")
            .pattern("IBI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('C', net.minecraft.world.item.Items.COMPARATOR)
            .define('T', Items.TRANSISTOR.get())
            .define('X', Items.BUS_INTERFACE.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.DISK_DRIVE.get())
            .pattern("IUI")
            .pattern("XTD")
            .pattern("IBI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('U', ItemTags.BUTTONS)
            .define('T', Items.TRANSISTOR.get())
            .define('X', Items.BUS_INTERFACE.get())
            .define('D', net.minecraft.world.item.Items.DISPENSER)
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.KEYBOARD.get())
            .pattern("UUU")
            .pattern("XTU")
            .pattern("IBI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('U', ItemTags.BUTTONS)
            .define('T', Items.TRANSISTOR.get())
            .define('X', Items.BUS_INTERFACE.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.CHARGER.get())
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
            .shaped(RecipeCategory.MISC, Items.PROJECTOR.get())
            .pattern("GLG")
            .pattern("XTD")
            .pattern("GBG")
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('L', net.minecraft.world.item.Items.REDSTONE_LAMP)
            .define('D', Tags.Items.GEMS_DIAMOND)
            .define('T', Items.TRANSISTOR.get())
            .define('X', Items.BUS_INTERFACE.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
            .save(consumer);


        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.WRENCH.get())
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
            .shaped(RecipeCategory.MISC, Items.NETWORK_CABLE.get(), 8)
            .pattern("SSS")
            .pattern("GTG")
            .pattern("SSS")
            .define('S', Tags.Items.STRINGS)
            .define('G', Tags.Items.GLASS_BLOCKS)
            .define('T', Items.TRANSISTOR.get())
            .unlockedBy("has_network_connector", inventoryChange(Items.NETWORK_CONNECTOR.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.ROBOT.get())
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
            .shaped(RecipeCategory.MISC, Items.MEMORY_SMALL.get(), 2)
            .pattern("ITI")
            .pattern(" B ")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.MEMORY_MEDIUM.get(), 2)
            .pattern("GTG")
            .pattern(" B ")
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.MEMORY_LARGE.get(), 2)
            .pattern("DTD")
            .pattern(" B ")
            .define('D', Tags.Items.GEMS_DIAMOND)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.HARD_DRIVE_SMALL.get())
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
            .shaped(RecipeCategory.MISC, Items.HARD_DRIVE_MEDIUM.get())
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
            .shaped(RecipeCategory.MISC, Items.HARD_DRIVE_LARGE.get())
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
            .shaped(RecipeCategory.MISC, Items.FLASH_MEMORY.get())
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

        SpecialRecipeBuilder
            .special(ResetRecipe::new)
            .save(consumer, API.MOD_ID + ":reset");

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.REDSTONE_INTERFACE_CARD.get())
            .pattern("IRT")
            .pattern(" B ")
            .define('R', net.minecraft.world.item.Items.REDSTONE_TORCH)
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.NETWORK_INTERFACE_CARD.get())
            .pattern("IGT")
            .pattern(" B ")
            .define('G', Tags.Items.GLASS_BLOCKS)
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.NETWORK_TUNNEL_CARD.get())
            .pattern("IET")
            .pattern(" B ")
            .define('E', Tags.Items.ENDER_PEARLS)
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.FILE_IMPORT_EXPORT_CARD.get())
            .pattern("IET")
            .pattern(" B ")
            .define('E', net.minecraft.world.item.Items.PAPER)
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.SOUND_CARD.get())
            .pattern("IST")
            .pattern(" B ")
            .define('S', net.minecraft.world.item.Items.NOTE_BLOCK)
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.FLOPPY.get())
            .pattern("ITI")
            .pattern("QBQ")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('Q', Tags.Items.GEMS_QUARTZ)
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_disk_drive", inventoryChange(Items.DISK_DRIVE.get()))
            .save(consumer);


        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.INVENTORY_OPERATIONS_MODULE.get())
            .pattern("TCG")
            .pattern(" B ")
            .define('T', Items.TRANSISTOR.get())
            .define('C', Tags.Items.CHESTS_WOODEN)
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.BLOCK_OPERATIONS_MODULE.get())
            .pattern("TPG")
            .pattern(" B ")
            .define('T', Items.TRANSISTOR.get())
            .define('P', net.minecraft.world.item.Items.DIAMOND_PICKAXE)
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.NETWORK_TUNNEL_MODULE.get())
            .pattern("TEG")
            .pattern(" B ")
            .define('T', Items.TRANSISTOR.get())
            .define('E', Tags.Items.ENDER_PEARLS)
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_robot", inventoryChange(Items.ROBOT.get()))
            .save(consumer);


        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.TRANSISTOR.get(), 12)
            .pattern("RCR")
            .pattern("III")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('R', Tags.Items.DUSTS_REDSTONE)
            .define('C', net.minecraft.world.item.Items.COMPARATOR)
            .unlockedBy("has_gold", inventoryChange(net.minecraft.world.item.Items.GOLD_INGOT))
            .save(consumer);

        ShapelessRecipeBuilder
            .shapeless(RecipeCategory.MISC, Items.CIRCUIT_BOARD.get(), 6)
            .requires(Tags.Items.INGOTS_GOLD)
            .requires(net.minecraft.world.item.Items.CLAY_BALL)
            .requires(Items.TRANSISTOR.get())
            .unlockedBy("has_transistor", inventoryChange(Items.TRANSISTOR.get()))
            .save(consumer);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(final ItemLike item) {
        return has(item);
    }
}
