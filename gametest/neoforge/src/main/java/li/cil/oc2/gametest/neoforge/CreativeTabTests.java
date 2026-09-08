/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

/**
 * Recipes yield plain items, and recipe viewers tell variants apart by their contents. So the plain
 * item has to be in the creative tab, or looking up what the tab offers finds no recipe for it.
 */
@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeTabTests {
    @GameTest(template = TEMPLATE)
    public static void machinesAreOfferedWithoutParts(final GameTestHelper helper) {
        requirePlainVariant(helper, Items.COMPUTER.get(), "computer");
        requirePlainVariant(helper, Items.ROBOT.get(), "robot");

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static void requirePlainVariant(final GameTestHelper helper, final Item item, final String what) {
        final List<ItemStack> offered = collect(helper, item);
        if (offered.isEmpty()) {
            throw new GameTestAssertException("the creative tab offers no " + what + " at all");
        }

        final boolean hasPlain = offered.stream().anyMatch(stack ->
            ItemStackUtils.getBlockEntityDataTag(stack).isEmpty()
                && ItemStackUtils.getModDataTag(stack).isEmpty());
        if (!hasPlain) {
            throw new GameTestAssertException("every " + what + " in the creative tab comes with parts in it, "
                + "so recipe viewers find no recipe for what the tab offers");
        }
    }

    private static List<ItemStack> collect(final GameTestHelper helper, final Item item) {
        final List<ItemStack> stacks = new ArrayList<>();
        final HolderLookup.Provider holders = helper.getLevel().registryAccess();
        final CreativeModeTab.ItemDisplayParameters parameters =
            new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, holders);

        ((li.cil.oc2.common.item.CreativeTabItemProvider) item).addCreativeTabItems(parameters,
            (stack, visibility) -> stacks.add(stack));

        return stacks;
    }

    // --------------------------------------------------------------------- //

    private CreativeTabTests() {
    }
}
