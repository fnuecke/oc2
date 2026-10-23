/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.VanillaDeviceTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class VanillaDeviceTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 600)
    public void signJoinsTheBus(final GameTestHelper helper) {
        VanillaDeviceTests.signJoinsTheBus(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 600)
    public void noteBlockKeepsItsDeviceWhenWritten(final GameTestHelper helper) {
        VanillaDeviceTests.noteBlockKeepsItsDeviceWhenWritten(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void signReadsAndWritesText(final GameTestHelper helper) {
        VanillaDeviceTests.signReadsAndWritesText(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void furnaceReportsProgress(final GameTestHelper helper) {
        VanillaDeviceTests.furnaceReportsProgress(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void brewingStandReportsFuel(final GameTestHelper helper) {
        VanillaDeviceTests.brewingStandReportsFuel(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void beaconReportsEffects(final GameTestHelper helper) {
        VanillaDeviceTests.beaconReportsEffects(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void spawnerReportsEntityType(final GameTestHelper helper) {
        VanillaDeviceTests.spawnerReportsEntityType(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void comparatorSwitchesMode(final GameTestHelper helper) {
        VanillaDeviceTests.comparatorSwitchesMode(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void jukeboxReportsSong(final GameTestHelper helper) {
        VanillaDeviceTests.jukeboxReportsSong(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void lecternReportsPages(final GameTestHelper helper) {
        VanillaDeviceTests.lecternReportsPages(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void crafterTogglesSlotsAndPredictsResult(final GameTestHelper helper) {
        VanillaDeviceTests.crafterTogglesSlotsAndPredictsResult(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void beehiveReportsHoney(final GameTestHelper helper) {
        VanillaDeviceTests.beehiveReportsHoney(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void composterReportsLevel(final GameTestHelper helper) {
        VanillaDeviceTests.composterReportsLevel(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void noteBlockTunes(final GameTestHelper helper) {
        VanillaDeviceTests.noteBlockTunes(helper);
    }
}
