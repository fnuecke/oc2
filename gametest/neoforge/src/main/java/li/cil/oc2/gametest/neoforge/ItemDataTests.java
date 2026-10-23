/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.NetworkInterfaceCardItem;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "item_data")
public final class ItemDataTests {
    private static final String KEY = "test_key";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void networkCardSideConfigurationPersists(final GameTestHelper helper) {
        final ItemStack card = new ItemStack(Items.NETWORK_INTERFACE_CARD.get());

        for (final Direction side : Direction.values()) {
            if (!NetworkInterfaceCardItem.getSideConfiguration(card, side)) {
                throw new GameTestAssertException(Component.literal("a fresh card should enable every side, but " + side + " was off"), 0);
            }
        }
        if (NetworkInterfaceCardItem.hasConfiguration(card)) {
            throw new GameTestAssertException(Component.literal("a fresh card should not report a configuration"), 0);
        }

        NetworkInterfaceCardItem.setSideConfiguration(card, Direction.UP, false);

        if (NetworkInterfaceCardItem.getSideConfiguration(card, Direction.UP)) {
            throw new GameTestAssertException(Component.literal("disabling a side did not persist on the stack"), 0);
        }
        if (!NetworkInterfaceCardItem.getSideConfiguration(card, Direction.NORTH)) {
            throw new GameTestAssertException(Component.literal("disabling one side must not disable the others"), 0);
        }
        if (!NetworkInterfaceCardItem.hasConfiguration(card)) {
            throw new GameTestAssertException(Component.literal("a configured card should report that it is configured"), 0);
        }

        NetworkInterfaceCardItem.setSideConfiguration(card, Direction.UP, true);

        if (!NetworkInterfaceCardItem.getSideConfiguration(card, Direction.UP)) {
            throw new GameTestAssertException(Component.literal("re-enabling a side did not persist on the stack"), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void modDataMutationPersists(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.FLASH_MEMORY.get());

        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putInt(KEY, 42));

        final int read = ItemStackUtils.getModDataTag(stack).getIntOr(KEY, 0);
        if (read != 42) {
            throw new GameTestAssertException(Component.literal("mod data did not persist; got " + read), 0);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void modDataGetterReturnsCopy(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.FLASH_MEMORY.get());

        final CompoundTag detached = ItemStackUtils.getModDataTag(stack);
        detached.putInt(KEY, 7);

        if (ItemStackUtils.getModDataTag(stack).contains(KEY)) {
            throw new GameTestAssertException(Component.literal(
                "getModDataTag returned a live tag; mutating it must not write through"), 0);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void blockEntityDataMutationPersists(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.COMPUTER.get());

        ItemStackUtils.modifyBlockEntityDataTag(stack, BlockEntities.COMPUTER.get(),
            tag -> tag.putInt(KEY, 13));

        final int read = ItemStackUtils.getBlockEntityDataTag(stack).getIntOr(KEY, 0);
        if (read != 13) {
            throw new GameTestAssertException(Component.literal(
                "modifyBlockEntityDataTag did not persist: expected 13, got " + read), 0);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void modDataAndBlockEntityDataAreDistinct(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.COMPUTER.get());

        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putInt(KEY, 1));
        ItemStackUtils.modifyBlockEntityDataTag(stack, BlockEntities.COMPUTER.get(),
            tag -> tag.putInt(KEY, 2));

        if (ItemStackUtils.getModDataTag(stack).getIntOr(KEY, 0) != 1) {
            throw new GameTestAssertException(Component.literal("block entity data leaked into mod data"), 0);
        }
        if (ItemStackUtils.getBlockEntityDataTag(stack).getIntOr(KEY, 0) != 2) {
            throw new GameTestAssertException(Component.literal("mod data leaked into block entity data"), 0);
        }
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private ItemDataTests() {
    }
}
