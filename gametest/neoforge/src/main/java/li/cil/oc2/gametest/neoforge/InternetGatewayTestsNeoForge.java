/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.InternetGatewayTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class InternetGatewayTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void gatewayPlacesAndOffersItsCapabilities(final GameTestHelper helper) {
        InternetGatewayTests.gatewayPlacesAndOffersItsCapabilities(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void gatewayWithoutAConnectionDropsFrames(final GameTestHelper helper) {
        InternetGatewayTests.gatewayWithoutAConnectionDropsFrames(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void gatewayKeepsItsEnergyAcrossAReload(final GameTestHelper helper) {
        InternetGatewayTests.gatewayKeepsItsEnergyAcrossAReload(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void gatewayWithoutInternetAccessIsNotOperational(final GameTestHelper helper) {
        InternetGatewayTests.gatewayWithoutInternetAccessIsNotOperational(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void gatewayTracksItsOperationalStateFromEnergy(final GameTestHelper helper) {
        InternetGatewayTests.gatewayTracksItsOperationalStateFromEnergy(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void gatewayReleasesItsCapabilitiesWhenBroken(final GameTestHelper helper) {
        InternetGatewayTests.gatewayReleasesItsCapabilitiesWhenBroken(helper);
    }
}
