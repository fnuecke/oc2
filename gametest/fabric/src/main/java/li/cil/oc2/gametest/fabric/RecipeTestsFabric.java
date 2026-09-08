/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.RecipeTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class RecipeTestsFabric {
    @GameTest(template = TEMPLATE)
    public void everyModItemIsCraftable(final GameTestHelper helper) {
        RecipeTests.everyModItemIsCraftable(helper);
    }

    @GameTest(template = TEMPLATE)
    public void everyRecipeCraftsInCraftingTable(final GameTestHelper helper) {
        RecipeTests.everyRecipeCraftsInCraftingTable(helper);
    }

    @GameTest(template = TEMPLATE)
    public void noRecipeShadowsAnother(final GameTestHelper helper) {
        RecipeTests.noRecipeShadowsAnother(helper);
    }
}
