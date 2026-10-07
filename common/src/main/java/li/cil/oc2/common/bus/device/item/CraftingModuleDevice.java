/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

@RPCDeviceDescription(typeName = "crafting", description = """
    Provided by the [crafting module](../item/crafting_module.md) to robots.

    The top part of the robot's inventory, slots `0` to `8`, serves as the crafting grid. Crafted items go into the bottom row, starting at the selected slot.""")
@IODeviceDescription(name = "CRFTNG")
public final class CraftingModuleDevice extends AbstractItemDevice {
    private static final int CRAFT_CODE = 1;

    private static final int GRID_SIZE = 3;
    private static final int GRID_SLOTS = GRID_SIZE * GRID_SIZE;

    private static final String CRAFT_DESCRIPTION = "Tries to craft once from the crafting grid. Fails if the grid matches no recipe " +
        "or the result does not fit into the bottom row.";

    // --------------------------------------------------------------------- //

    private final Entity entity;
    private final Robot robot;

    // --------------------------------------------------------------------- //

    public CraftingModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity);
        this.entity = entity;
        this.robot = robot;
    }

    // --------------------------------------------------------------------- //

    @Callback(energy = 1,
        description = CRAFT_DESCRIPTION,
        returnValueDescription = "the number of items crafted, or `0` if crafting failed.")
    public int craft() {
        final Level level = entity.level();
        final ItemHandler inventory = inventory();

        final CraftingInput.Positioned positioned = CraftingInput.ofPositioned(GRID_SIZE, GRID_SIZE, gridItems(inventory));
        final CraftingInput input = positioned.input();
        final Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isEmpty()) {
            return 0;
        }

        final ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        if (result.isEmpty() || !insertIntoOutput(inventory, result, true).isEmpty()) {
            return 0;
        }

        final NonNullList<ItemStack> remainders = recipe.get().value().getRemainingItems(input);
        result.onCraftedBySystem(level);
        final int crafted = result.getCount();
        insertIntoOutput(inventory, result, false);
        consumeIngredients(inventory, positioned, remainders);

        return crafted;
    }

    // --------------------------------------------------------------------- //

    @IOCallback(energy = 1, value = CRAFT_CODE,
        description = CRAFT_DESCRIPTION,
        resultsDescription = "one byte, the number of items crafted, `0` if crafting failed.")
    public void craft(final IOOutputStream results) throws IOException {
        results.writeU8(craft());
    }

    // --------------------------------------------------------------------- //

    private List<ItemStack> gridItems(final ItemHandler inventory) {
        return IntStream.range(0, GRID_SLOTS).mapToObj(inventory::getStackInSlot).toList();
    }

    private void consumeIngredients(final ItemHandler inventory, final CraftingInput.Positioned positioned,
                                    final NonNullList<ItemStack> remainders) {
        final CraftingInput input = positioned.input();
        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column < input.width(); column++) {
                final int slot = positioned.left() + column + (positioned.top() + row) * GRID_SIZE;
                inventory.extractItem(slot, 1, false);

                final ItemStack remainder = remainders.get(column + row * input.width());
                if (remainder.isEmpty()) {
                    continue;
                }

                final ItemStack rejected = insertIntoOutput(inventory, inventory.insertItem(slot, remainder, false), false);
                if (!rejected.isEmpty()) {
                    entity.spawnAtLocation(rejected);
                }
            }
        }
    }

    private ItemStack insertIntoOutput(final ItemHandler inventory, ItemStack stack, final boolean simulate) {
        final int outputSlots = inventory.getSlots() - GRID_SLOTS;
        final int start = Math.max(robot.getSelectedSlot() - GRID_SLOTS, 0);
        for (int i = 0; i < outputSlots && !stack.isEmpty(); i++) {
            stack = inventory.insertItem(GRID_SLOTS + (start + i) % outputSlots, stack, simulate);
        }

        return stack;
    }

    private ItemHandler inventory() {
        return Objects.requireNonNull(Capabilities.get(entity, Capabilities.ITEM_HANDLER, null));
    }
}
