/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.InternetGatewayTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "internet_gateway")
public final class InternetGatewayTestsNeoForge {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300, batch = "oc2_gateway_offline")
    @TestHolder
    public static void gatewayWithoutAConnectionDropsFrames(final GameTestHelper helper) {
        InternetGatewayTests.gatewayWithoutAConnectionDropsFrames(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void gatewayKeepsItsEnergyAcrossAReload(final GameTestHelper helper) {
        InternetGatewayTests.gatewayKeepsItsEnergyAcrossAReload(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300, batch = "oc2_gateway_no_access")
    @TestHolder
    public static void gatewayWithoutInternetAccessIsNotOperational(final GameTestHelper helper) {
        InternetGatewayTests.gatewayWithoutInternetAccessIsNotOperational(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300, batch = "oc2_gateway_online")
    @TestHolder
    public static void gatewayTracksItsOperationalStateFromEnergy(final GameTestHelper helper) {
        InternetGatewayTests.gatewayTracksItsOperationalStateFromEnergy(helper);
    }

    // --------------------------------------------------------------------- //

    private InternetGatewayTestsNeoForge() {
    }
}
