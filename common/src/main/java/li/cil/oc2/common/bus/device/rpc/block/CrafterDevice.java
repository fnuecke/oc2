/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.block;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.common.bus.device.util.ItemHandlerProtocol;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;

import java.io.IOException;

@RPCDeviceDescription(typeNames = {"crafter"}, description = """
    Provided by crafters connected to a [bus interface](../block/bus_interface.md). The grid slots are available through the [item handler](item_handler.md) device.""")
@IODeviceDescription(name = "CRAFTR", description = """
    Items are identified by the same ids as on `ITEMS`, which also turns them into names.""")
public final class CrafterDevice extends AbstractBlockDevice {
    private static final int IS_SLOT_ENABLED_CODE = 1;
    private static final int SET_SLOT_ENABLED_CODE = 2;
    private static final int GET_RESULT_CODE = 3;

    private static final int SLOT_COUNT = 9;

    // --------------------------------------------------------------------- //

    public CrafterDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets whether a slot of the grid is enabled.",
        returnValueDescription = "`true` if the slot is enabled.")
    public boolean isSlotEnabled(@Parameter(value = "slot", description = "the slot index.") final int slot) {
        return !getCrafter().isSlotDisabled(requireValidSlot(slot));
    }

    @Callback(description = "Sets whether a grid slot is enabled. Slot must be empty to be disabled.")
    public void setSlotEnabled(@Parameter(value = "slot", description = "the slot index.") final int slot,
                               @Parameter(value = "enabled", description = "whether to enable the slot.") final boolean enabled) {
        final CrafterBlockEntity crafter = getCrafter();
        if (!enabled && !crafter.getItem(requireValidSlot(slot)).isEmpty()) {
            throw new IllegalStateException("slot is not empty");
        }

        checkPermission();
        crafter.setSlotState(requireValidSlot(slot), enabled);
    }

    @Callback(description = "Gets what the crafter would currently craft.",
        returnValueDescription = "a table with the item information. Returns nothing if the grid matches no recipe.")
    public ItemStack getResult() {
        final CrafterBlockEntity crafter = getCrafter();
        final ServerLevel level = getServerLevel();
        final CraftingInput input = crafter.asCraftInput();
        return CrafterBlock.getPotentialResults(level, input)
            .map(recipe -> recipe.value().assemble(input, level.registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = IS_SLOT_ENABLED_CODE,
        description = "Gets whether a slot of the grid is enabled.",
        argumentsDescription = "one byte, the slot index.",
        resultsDescription = "one byte, `1` if enabled, else `0`.")
    public void isSlotEnabled(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(isSlotEnabled(arguments.readU8()) ? 1 : 0);
    }

    @IOCallback(value = SET_SLOT_ENABLED_CODE,
        description = "Sets whether a grid slot is enabled. Slot must be empty to be disabled.",
        argumentsDescription = "two bytes, the slot index, and `0` to disable or `1` to enable it.")
    public void setSlotEnabled(final IOInputStream arguments) throws IOException {
        final int slot = arguments.readU8();
        setSlotEnabled(slot, arguments.readU8() != 0);
    }

    @IOCallback(value = GET_RESULT_CODE,
        description = "Gets what the crafter would currently craft.",
        resultsDescription = "three bytes: the item as two bytes, `0` if the grid matches no recipe, else the number of items.")
    public void getResult(final IOOutputStream results) throws IOException {
        final ItemStack result = getResult();
        results.writeU16(ItemHandlerProtocol.toItemId(result.getItem()));
        results.writeU8(Math.min(result.getCount(), 0xFF));
    }

    // --------------------------------------------------------------------- //

    private static int requireValidSlot(final int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IllegalArgumentException("slot out of range: " + slot + " (expected 0 to " + (SLOT_COUNT - 1) + ")");
        }
        return slot;
    }

    private CrafterBlockEntity getCrafter() {
        return getBlockEntity(CrafterBlockEntity.class);
    }
}
