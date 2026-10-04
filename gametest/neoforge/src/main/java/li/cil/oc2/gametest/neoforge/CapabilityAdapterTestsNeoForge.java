/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.CapabilityAdapterTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.CapabilityAdapterTests.ENERGY;
import static li.cil.oc2.gametest.CapabilityAdapterTests.ITEMS;
import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class CapabilityAdapterTestsNeoForge {
    @GameTest(template = TEMPLATE)
    public static void simulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedInsertDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void committedInsertMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedInsertMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void simulatedExtractDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedExtractDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void committedExtractMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedExtractMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void insertClampsToCapacity(final GameTestHelper helper) {
        CapabilityAdapterTests.insertClampsToCapacity(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void extractClampsToContents(final GameTestHelper helper) {
        CapabilityAdapterTests.extractClampsToContents(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void simulatedItemInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedItemInsertDoesNotMutate(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public static void committedItemInsertMutates(final GameTestHelper helper) {
        CapabilityAdapterTests.committedItemInsertMutates(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public static void itemRoundTripPreservesIdentity(final GameTestHelper helper) {
        CapabilityAdapterTests.itemRoundTripPreservesIdentity(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public static void slotCapabilityWritesToInventory(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityWritesToInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerItemSimulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemSimulatedInsertDoesNotMutate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerItemInsertAndExtract(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertAndExtract(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerItemInsertReturnsRemainder(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertReturnsRemainder(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerFluidSimulatedFillDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidSimulatedFillDoesNotMutate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerFluidFillReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillReplacesItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerFluidFillSplitsStack(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillSplitsStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerFluidDrainReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidDrainReplacesItem(helper);
    }

    // --------------------------------------------------------------------- //

    private CapabilityAdapterTestsNeoForge() {
    }
}
