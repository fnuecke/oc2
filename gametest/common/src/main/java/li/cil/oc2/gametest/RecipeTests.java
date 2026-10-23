/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

import static li.cil.oc2.gametest.util.TestSupport.*;

public final class RecipeTests {
    private static final BlockPos TABLE = new BlockPos(1, WORK_Y, 1);
    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;

    private static final Set<String> ITEMS_WITHOUT_RECIPE = Set.of(
        "creative_energy"
    );

    // --------------------------------------------------------------------- //

    public static void everyModItemIsCraftable(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RecipeManager recipes = level.getServer().getRecipeManager();
        final ContextMap displayContext = SlotDisplayContext.fromLevel(level);

        final List<Item> modItems = BuiltInRegistries.ITEM.entrySet().stream()
            .filter(entry -> entry.getKey().identifier().getNamespace().equals(API.MOD_ID))
            .map(Map.Entry::getValue)
            .toList();

        assertTrue(helper, "expected the mod to register items, found none", !modItems.isEmpty());

        helper.setBlock(TABLE, Blocks.CRAFTING_TABLE);

        final List<String> withoutRecipe = new ArrayList<>();
        for (final Item item : modItems) {
            final Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (ITEMS_WITHOUT_RECIPE.contains(id.getPath())) {
                continue;
            }

            final List<RecipeHolder<?>> producing = recipes.getRecipes().stream()
                .filter(holder -> holder.value() instanceof CraftingRecipe)
                .filter(holder -> produces(holder, displayContext, item))
                .toList();

            if (producing.isEmpty()) {
                withoutRecipe.add(id.getPath());
                continue;
            }

            for (final RecipeHolder<?> holder : producing) {
                craft(helper, level, displayContext, holder);
            }
        }

        if (!withoutRecipe.isEmpty()) {
            throw failure(helper, "no crafting recipe for " + withoutRecipe
                + "; add recipes, or list them in ITEMS_WITHOUT_RECIPE");
        }

        helper.succeed();
    }

    public static void everyRecipeCraftsInCraftingTable(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RecipeManager recipes = level.getServer().getRecipeManager();
        final ContextMap displayContext = SlotDisplayContext.fromLevel(level);

        final List<RecipeHolder<?>> modRecipes = recipes.getRecipes().stream()
            .filter(holder -> holder.id().identifier().getNamespace().equals(API.MOD_ID))
            .filter(holder -> !holder.value().isSpecial())
            .toList();

        assertTrue(helper, "expected the mod to contribute recipes, found none", !modRecipes.isEmpty());

        helper.setBlock(TABLE, Blocks.CRAFTING_TABLE);

        for (final RecipeHolder<?> holder : modRecipes) {
            craft(helper, level, displayContext, holder);
        }

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static boolean produces(final RecipeHolder<?> holder, final ContextMap displayContext, final Item item) {
        return holder.value().display().stream()
            .anyMatch(display -> display.result().resolveForFirstStack(displayContext).getItem() == item);
    }

    private static void craft(final GameTestHelper helper, final ServerLevel level, final ContextMap displayContext, final RecipeHolder<?> holder) {
        final String id = holder.id().identifier().toString();

        if (!(holder.value() instanceof final CraftingRecipe recipe)) {
            throw failure(helper, id + " is not a crafting recipe (" + holder.value().getClass().getSimpleName() + ")");
        }

        final List<RecipeDisplay> displays = recipe.display();
        if (displays.isEmpty()) {
            throw failure(helper, id + " has no recipe display to build inputs from");
        }

        final RecipeDisplay display = displays.getFirst();
        final ItemStack expected = display.result().resolveForFirstStack(displayContext);
        assertTrue(helper, id + " should produce a result", !expected.isEmpty());

        @SuppressWarnings("removal") final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final CraftingMenu menu = new CraftingMenu(0, player.getInventory(),
            ContainerLevelAccess.create(level, helper.absolutePos(TABLE)));
        final List<Slot> grid = menu.getInputGridSlots();

        final List<ItemStack> inputs = gridInputs(helper, id, display, displayContext);
        for (int slot = 0; slot < inputs.size(); slot++) {
            grid.get(slot).set(inputs.get(slot));
        }
        menu.slotsChanged(grid.getFirst().container);

        final ItemStack result = menu.getResultSlot().getItem();
        if (result.isEmpty()) {
            throw failure(helper, id + " did not match its own ingredients: " + describe(inputs));
        }
        assertTrue(helper, id + " should craft " + expected + ", got " + result,
            ItemStack.isSameItemSameComponents(expected, result));
        assertEquals(helper, id + " result count", expected.getCount(), result.getCount());

        final List<ItemStack> expectedLeftovers =
            recipe.getRemainingItems(CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, inputs));

        menu.quickMoveStack(player, menu.getResultSlot().index);

        for (int slot = 0; slot < expectedLeftovers.size(); slot++) {
            final ItemStack expectedLeftover = expectedLeftovers.get(slot);
            final ItemStack leftover = grid.get(slot).getItem();
            assertTrue(helper, id + " should leave " + expectedLeftover + " in grid slot " + slot + ", found " + leftover,
                ItemStack.matches(expectedLeftover, leftover));
        }
    }

    private static List<ItemStack> gridInputs(final GameTestHelper helper, final String id, final RecipeDisplay display, final ContextMap displayContext) {
        final List<ItemStack> inputs = new ArrayList<>(GRID_WIDTH * GRID_HEIGHT);
        for (int i = 0; i < GRID_WIDTH * GRID_HEIGHT; i++) {
            inputs.add(ItemStack.EMPTY);
        }

        switch (display) {
            case final ShapedCraftingRecipeDisplay shaped -> {
                if (shaped.width() > GRID_WIDTH || shaped.height() > GRID_HEIGHT) {
                    throw failure(helper, id + " does not fit a " + GRID_WIDTH + "x" + GRID_HEIGHT + " grid");
                }
                final List<SlotDisplay> ingredients = shaped.ingredients();
                for (int y = 0; y < shaped.height(); y++) {
                    for (int x = 0; x < shaped.width(); x++) {
                        inputs.set(y * GRID_WIDTH + x,
                            ingredients.get(y * shaped.width() + x).resolveForFirstStack(displayContext).copyWithCount(1));
                    }
                }
            }
            case final ShapelessCraftingRecipeDisplay shapeless -> {
                final List<SlotDisplay> ingredients = shapeless.ingredients();
                if (ingredients.size() > inputs.size()) {
                    throw failure(helper, id + " has more ingredients than the grid has slots");
                }
                for (int i = 0; i < ingredients.size(); i++) {
                    inputs.set(i, ingredients.get(i).resolveForFirstStack(displayContext).copyWithCount(1));
                }
            }
            default -> throw failure(helper, id + " has an unsupported recipe display (" + display.getClass().getSimpleName() + ")");
        }

        return inputs;
    }

    private static String describe(final List<ItemStack> inputs) {
        return inputs.stream().map(stack -> stack.isEmpty() ? "-" : stack.getItem().toString()).toList().toString();
    }

    // --------------------------------------------------------------------- //

    private RecipeTests() {
    }
}
