/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.VanillaDeviceTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class VanillaDeviceTestsFabric {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void signJoinsTheBus(final GameTestHelper helper) {
        VanillaDeviceTests.signJoinsTheBus(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void noteBlockKeepsItsDeviceWhenWritten(final GameTestHelper helper) {
        VanillaDeviceTests.noteBlockKeepsItsDeviceWhenWritten(helper);
    }

    @GameTest(template = TEMPLATE)
    public void signReadsAndWritesText(final GameTestHelper helper) {
        VanillaDeviceTests.signReadsAndWritesText(helper);
    }

    @GameTest(template = TEMPLATE)
    public void furnaceReportsProgress(final GameTestHelper helper) {
        VanillaDeviceTests.furnaceReportsProgress(helper);
    }

    @GameTest(template = TEMPLATE)
    public void brewingStandReportsFuel(final GameTestHelper helper) {
        VanillaDeviceTests.brewingStandReportsFuel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void beaconReportsEffects(final GameTestHelper helper) {
        VanillaDeviceTests.beaconReportsEffects(helper);
    }

    @GameTest(template = TEMPLATE)
    public void spawnerReportsEntityType(final GameTestHelper helper) {
        VanillaDeviceTests.spawnerReportsEntityType(helper);
    }

    @GameTest(template = TEMPLATE)
    public void comparatorSwitchesMode(final GameTestHelper helper) {
        VanillaDeviceTests.comparatorSwitchesMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public void jukeboxReportsSong(final GameTestHelper helper) {
        VanillaDeviceTests.jukeboxReportsSong(helper);
    }

    @GameTest(template = TEMPLATE)
    public void lecternReportsPages(final GameTestHelper helper) {
        VanillaDeviceTests.lecternReportsPages(helper);
    }

    @GameTest(template = TEMPLATE)
    public void crafterTogglesSlotsAndPredictsResult(final GameTestHelper helper) {
        VanillaDeviceTests.crafterTogglesSlotsAndPredictsResult(helper);
    }

    @GameTest(template = TEMPLATE)
    public void beehiveReportsHoney(final GameTestHelper helper) {
        VanillaDeviceTests.beehiveReportsHoney(helper);
    }

    @GameTest(template = TEMPLATE)
    public void composterReportsLevel(final GameTestHelper helper) {
        VanillaDeviceTests.composterReportsLevel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void noteBlockTunes(final GameTestHelper helper) {
        VanillaDeviceTests.noteBlockTunes(helper);
    }
}
