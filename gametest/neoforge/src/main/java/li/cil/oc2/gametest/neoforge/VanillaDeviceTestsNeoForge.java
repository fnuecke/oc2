/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.VanillaDeviceTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class VanillaDeviceTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void signJoinsTheBus(final GameTestHelper helper) {
        VanillaDeviceTests.signJoinsTheBus(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void noteBlockKeepsItsDeviceWhenWritten(final GameTestHelper helper) {
        VanillaDeviceTests.noteBlockKeepsItsDeviceWhenWritten(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void signReadsAndWritesText(final GameTestHelper helper) {
        VanillaDeviceTests.signReadsAndWritesText(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void furnaceReportsProgress(final GameTestHelper helper) {
        VanillaDeviceTests.furnaceReportsProgress(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void brewingStandReportsFuel(final GameTestHelper helper) {
        VanillaDeviceTests.brewingStandReportsFuel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void beaconReportsEffects(final GameTestHelper helper) {
        VanillaDeviceTests.beaconReportsEffects(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spawnerReportsEntityType(final GameTestHelper helper) {
        VanillaDeviceTests.spawnerReportsEntityType(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comparatorSwitchesMode(final GameTestHelper helper) {
        VanillaDeviceTests.comparatorSwitchesMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void jukeboxReportsSong(final GameTestHelper helper) {
        VanillaDeviceTests.jukeboxReportsSong(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void lecternReportsPages(final GameTestHelper helper) {
        VanillaDeviceTests.lecternReportsPages(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crafterTogglesSlotsAndPredictsResult(final GameTestHelper helper) {
        VanillaDeviceTests.crafterTogglesSlotsAndPredictsResult(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void beehiveReportsHoney(final GameTestHelper helper) {
        VanillaDeviceTests.beehiveReportsHoney(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void composterReportsLevel(final GameTestHelper helper) {
        VanillaDeviceTests.composterReportsLevel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void noteBlockTunes(final GameTestHelper helper) {
        VanillaDeviceTests.noteBlockTunes(helper);
    }
}
