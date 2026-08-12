/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemDataTests {
    private static final String KEY = "test_key";

    @GameTest(template = TEMPLATE)
    public static void modDataMutationPersists(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.FLASH_MEMORY.get());

        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putInt(KEY, 42));

        final int read = ItemStackUtils.getModDataTag(stack).getInt(KEY);
        if (read != 42) {
            throw new GameTestAssertException(
                "modifyModDataTag did not persist: expected 42, got " + read);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void modDataGetterReturnsCopy(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.FLASH_MEMORY.get());

        final CompoundTag detached = ItemStackUtils.getModDataTag(stack);
        detached.putInt(KEY, 7);

        if (ItemStackUtils.getModDataTag(stack).contains(KEY)) {
            throw new GameTestAssertException(
                "getModDataTag returned a live tag — mutating it must not write through, or the "
                    + "copy-mutation bugs stop being detectable");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blockEntityDataMutationPersists(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.COMPUTER.get());

        ItemStackUtils.modifyBlockEntityDataTag(stack, BlockEntities.COMPUTER.get(),
            tag -> tag.putInt(KEY, 13));

        final int read = ItemStackUtils.getBlockEntityDataTag(stack).getInt(KEY);
        if (read != 13) {
            throw new GameTestAssertException(
                "modifyBlockEntityDataTag did not persist: expected 13, got " + read);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void modDataAndBlockEntityDataAreDistinct(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.COMPUTER.get());

        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putInt(KEY, 1));
        ItemStackUtils.modifyBlockEntityDataTag(stack, BlockEntities.COMPUTER.get(),
            tag -> tag.putInt(KEY, 2));

        if (ItemStackUtils.getModDataTag(stack).getInt(KEY) != 1) {
            throw new GameTestAssertException("block entity data leaked into mod data");
        }
        if (ItemStackUtils.getBlockEntityDataTag(stack).getInt(KEY) != 2) {
            throw new GameTestAssertException("mod data leaked into block entity data");
        }
        helper.succeed();
    }

    private ItemDataTests() {
    }
}
