/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.RecipeTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeTestsNeoForge {
    @GameTest(template = TEMPLATE)
    public static void everyModItemIsCraftable(final GameTestHelper helper) {
        RecipeTests.everyModItemIsCraftable(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void everyRecipeCraftsInCraftingTable(final GameTestHelper helper) {
        RecipeTests.everyRecipeCraftsInCraftingTable(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void noRecipeShadowsAnother(final GameTestHelper helper) {
        RecipeTests.noRecipeShadowsAnother(helper);
    }

    // --------------------------------------------------------------------- //

    private RecipeTestsNeoForge() {
    }
}
