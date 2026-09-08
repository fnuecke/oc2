/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static li.cil.oc2.gametest.util.TestSupport.*;

public final class RecipeTests {
    private static final BlockPos TABLE = new BlockPos(1, WORK_Y, 1);
    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;

    // CraftingMenu holds the result in slot 0 and the crafting grid in the slots right after it.
    private static final int FIRST_GRID_SLOT = 1;

    private static final Set<String> ITEMS_WITHOUT_RECIPE = Set.of(
        // Creative only.
        "creative_energy"
    );

    // --------------------------------------------------------------------- //

    public static void everyModItemIsCraftable(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RecipeManager recipes = level.getServer().getRecipeManager();

        final List<Item> modItems = BuiltInRegistries.ITEM.entrySet().stream()
            .filter(entry -> entry.getKey().location().getNamespace().equals(API.MOD_ID))
            .map(Map.Entry::getValue)
            .toList();

        assertTrue(helper, "expected the mod to register items, found none", !modItems.isEmpty());

        helper.setBlock(TABLE, Blocks.CRAFTING_TABLE);

        final List<String> withoutRecipe = new ArrayList<>();
        for (final Item item : modItems) {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (ITEMS_WITHOUT_RECIPE.contains(id.getPath())) {
                continue;
            }

            final List<RecipeHolder<?>> producing = recipes.getRecipes().stream()
                .filter(holder -> holder.value() instanceof CraftingRecipe)
                .filter(holder -> holder.value().getResultItem(level.registryAccess()).getItem() == item)
                .toList();

            if (producing.isEmpty()) {
                withoutRecipe.add(id.getPath());
                continue;
            }

            for (final RecipeHolder<?> holder : producing) {
                craft(helper, level, holder);
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

        final List<RecipeHolder<?>> modRecipes = recipes.getRecipes().stream()
            .filter(holder -> holder.id().getNamespace().equals(API.MOD_ID))
            .filter(holder -> !holder.value().isSpecial())
            .toList();

        assertTrue(helper, "expected the mod to contribute recipes, found none", !modRecipes.isEmpty());

        helper.setBlock(TABLE, Blocks.CRAFTING_TABLE);

        for (final RecipeHolder<?> holder : modRecipes) {
            craft(helper, level, holder);
        }

        helper.succeed();
    }

    public static void noRecipeShadowsAnother(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RecipeManager recipes = level.getServer().getRecipeManager();

        final Map<ResourceLocation, Layout> layouts = new LinkedHashMap<>();
        for (final RecipeHolder<?> holder : recipes.getRecipes()) {
            if (!(holder.value() instanceof final CraftingRecipe recipe) || recipe.isSpecial()) {
                continue;
            }
            final Layout layout = layoutOf(recipe);
            if (layout != null) {
                layouts.put(holder.id(), layout);
            }
        }

        final List<ResourceLocation> modRecipes = layouts.keySet().stream()
            .filter(id -> id.getNamespace().equals(API.MOD_ID))
            .toList();

        assertTrue(helper, "expected the mod to contribute recipes, found none", !modRecipes.isEmpty());

        final List<String> conflicts = new ArrayList<>();
        for (final ResourceLocation id : modRecipes) {
            final Layout layout = layouts.get(id);
            for (final Map.Entry<ResourceLocation, Layout> entry : layouts.entrySet()) {
                final ResourceLocation otherId = entry.getKey();
                if (otherId.equals(id) || (otherId.getNamespace().equals(API.MOD_ID) && otherId.compareTo(id) < 0)) {
                    continue;
                }
                if (overlaps(layout, entry.getValue())) {
                    conflicts.add(id + " <-> " + otherId);
                }
            }
        }

        if (!conflicts.isEmpty()) {
            throw failure(helper, "recipes accept the same grid, so only the first one registered is craftable: " + conflicts);
        }

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static void craft(final GameTestHelper helper, final ServerLevel level, final RecipeHolder<?> holder) {
        final String id = holder.id().toString();

        if (!(holder.value() instanceof final CraftingRecipe recipe)) {
            throw failure(helper, id + " is not a crafting recipe (" + holder.value().getClass().getSimpleName() + ")");
        }

        final ItemStack expected = recipe.getResultItem(level.registryAccess());
        assertTrue(helper, id + " should produce a result", !expected.isEmpty());

        @SuppressWarnings("removal") final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final CraftingMenu menu = new CraftingMenu(0, player.getInventory(),
            ContainerLevelAccess.create(level, helper.absolutePos(TABLE)));

        final List<ItemStack> inputs = gridInputs(helper, id, recipe);
        for (int slot = 0; slot < inputs.size(); slot++) {
            menu.getSlot(FIRST_GRID_SLOT + slot).set(inputs.get(slot));
        }
        menu.slotsChanged(menu.getSlot(FIRST_GRID_SLOT).container);

        final ItemStack result = menu.getSlot(menu.getResultSlotIndex()).getItem();
        if (result.isEmpty()) {
            throw failure(helper, id + " did not match its own ingredients: " + describe(inputs));
        }
        assertTrue(helper, id + " should craft " + expected + ", got " + result,
            ItemStack.isSameItemSameComponents(expected, result));
        assertEquals(helper, id + " result count", expected.getCount(), result.getCount());

        final List<ItemStack> expectedLeftovers =
            recipe.getRemainingItems(CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, inputs));

        menu.quickMoveStack(player, menu.getResultSlotIndex());

        for (int slot = 0; slot < expectedLeftovers.size(); slot++) {
            final ItemStack expectedLeftover = expectedLeftovers.get(slot);
            final ItemStack leftover = menu.getSlot(FIRST_GRID_SLOT + slot).getItem();
            assertTrue(helper, id + " should leave " + expectedLeftover + " in grid slot " + slot + ", found " + leftover,
                ItemStack.matches(expectedLeftover, leftover));
        }
    }

    private static List<ItemStack> gridInputs(final GameTestHelper helper, final String id, final CraftingRecipe recipe) {
        final List<ItemStack> inputs = new ArrayList<>(GRID_WIDTH * GRID_HEIGHT);
        for (int i = 0; i < GRID_WIDTH * GRID_HEIGHT; i++) {
            inputs.add(ItemStack.EMPTY);
        }

        final List<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof final ShapedRecipe shaped) {
            if (shaped.getWidth() > GRID_WIDTH || shaped.getHeight() > GRID_HEIGHT) {
                throw failure(helper, id + " does not fit a " + GRID_WIDTH + "x" + GRID_HEIGHT + " grid");
            }
            for (int y = 0; y < shaped.getHeight(); y++) {
                for (int x = 0; x < shaped.getWidth(); x++) {
                    inputs.set(y * GRID_WIDTH + x,
                        firstStack(helper, id, ingredients.get(y * shaped.getWidth() + x)));
                }
            }
        } else {
            if (ingredients.size() > inputs.size()) {
                throw failure(helper, id + " has more ingredients than the grid has slots");
            }
            for (int i = 0; i < ingredients.size(); i++) {
                inputs.set(i, firstStack(helper, id, ingredients.get(i)));
            }
        }

        return inputs;
    }

    private static ItemStack firstStack(final GameTestHelper helper, final String id, final Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final ItemStack[] candidates = ingredient.getItems();
        if (candidates.length == 0) {
            throw failure(helper, id + " has an ingredient no item satisfies");
        }

        return candidates[0].copyWithCount(1);
    }

    private static String describe(final List<ItemStack> inputs) {
        return inputs.stream().map(stack -> stack.isEmpty() ? "-" : stack.getItem().toString()).toList().toString();
    }

    @Nullable
    private static Layout layoutOf(final CraftingRecipe recipe) {
        if (recipe.getIngredients().stream().anyMatch(RecipeTests::isUnsatisfiable)) {
            return null;
        }

        if (recipe instanceof final ShapedRecipe shaped) {
            return new Layout(shaped.getWidth(), shaped.getHeight(),
                shaped.getIngredients().stream().map(RecipeTests::itemsOf).toList());
        }

        final List<Set<Item>> ingredients = recipe.getIngredients().stream().map(RecipeTests::itemsOf).toList();
        return ingredients.isEmpty() ? null : new Layout(0, 0, ingredients);
    }

    private static boolean isUnsatisfiable(final Ingredient ingredient) {
        return !ingredient.isEmpty() && ingredient.getItems().length == 0;
    }

    private static Set<Item> itemsOf(final Ingredient ingredient) {
        return ingredient.isEmpty()
            ? Set.of()
            : Arrays.stream(ingredient.getItems()).map(ItemStack::getItem).collect(Collectors.toSet());
    }

    private static boolean overlaps(final Layout a, final Layout b) {
        if (!a.isShapeless() && !b.isShapeless()) {
            // Shaped recipes match mirrored as well, so both orientations have to be ruled out.
            return shapedOverlaps(a, b, false) || shapedOverlaps(a, b, true);
        }
        return anyOrderOverlaps(a.occupiedCells(), b.occupiedCells());
    }

    private static boolean shapedOverlaps(final Layout a, final Layout b, final boolean mirrored) {
        if (a.width() != b.width() || a.height() != b.height()) {
            return false;
        }

        for (int y = 0; y < a.height(); y++) {
            for (int x = 0; x < a.width(); x++) {
                final Set<Item> cellA = a.cells().get(y * a.width() + x);
                final Set<Item> cellB = b.cells().get(y * b.width() + (mirrored ? b.width() - 1 - x : x));
                if (cellA.isEmpty() != cellB.isEmpty() || (!cellA.isEmpty() && Collections.disjoint(cellA, cellB))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean anyOrderOverlaps(final List<Set<Item>> a, final List<Set<Item>> b) {
        return a.size() == b.size() && pairsUp(a, b, 0, new boolean[b.size()]);
    }

    private static boolean pairsUp(final List<Set<Item>> a, final List<Set<Item>> b, final int index, final boolean[] taken) {
        if (index == a.size()) {
            return true;
        }

        for (int i = 0; i < b.size(); i++) {
            if (taken[i] || Collections.disjoint(a.get(index), b.get(i))) {
                continue;
            }
            taken[i] = true;
            if (pairsUp(a, b, index + 1, taken)) {
                return true;
            }
            taken[i] = false;
        }

        return false;
    }

    // --------------------------------------------------------------------- //

    private RecipeTests() {
    }

    // --------------------------------------------------------------------- //

    // Cells hold the items each grid slot accepts; an empty set is an empty slot. Shapeless
    // recipes have no dimensions and carry only their ingredients.
    private record Layout(int width, int height, List<Set<Item>> cells) {
        boolean isShapeless() {
            return width == 0;
        }

        List<Set<Item>> occupiedCells() {
            return cells.stream().filter(cell -> !cell.isEmpty()).toList();
        }
    }
}
