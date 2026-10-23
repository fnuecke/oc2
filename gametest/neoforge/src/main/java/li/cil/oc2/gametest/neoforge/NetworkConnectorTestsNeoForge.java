/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.NetworkConnectorTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "network_connector")
public final class NetworkConnectorTestsNeoForge {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void connectorsAttachToPostsAndNothingElse(final GameTestHelper helper) {
        NetworkConnectorTests.connectorsAttachToPostsAndNothingElse(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void connectorsLinkWithClearLineOfSight(final GameTestHelper helper) {
        NetworkConnectorTests.connectorsLinkWithClearLineOfSight(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void connectorsRefuseObstructedLink(final GameTestHelper helper) {
        NetworkConnectorTests.connectorsRefuseObstructedLink(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void connectorsRefuseLinkBeyondRange(final GameTestHelper helper) {
        NetworkConnectorTests.connectorsRefuseLinkBeyondRange(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void removingAConnectorUnlinksItsPeer(final GameTestHelper helper) {
        NetworkConnectorTests.removingAConnectorUnlinksItsPeer(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void silentlyRemovedConnectorKeepsItsPeer(final GameTestHelper helper) {
        NetworkConnectorTests.silentlyRemovedConnectorKeepsItsPeer(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void stateChangeKeepsTheLink(final GameTestHelper helper) {
        NetworkConnectorTests.stateChangeKeepsTheLink(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void networkCableLinksConnectorsWhenUsed(final GameTestHelper helper) {
        NetworkConnectorTests.networkCableLinksConnectorsWhenUsed(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void connectorResolvesNetworkCard(final GameTestHelper helper) {
        NetworkConnectorTests.connectorResolvesNetworkCard(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void connectorDropsRemovedCard(final GameTestHelper helper) {
        NetworkConnectorTests.connectorDropsRemovedCard(helper);
    }

    // --------------------------------------------------------------------- //

    private NetworkConnectorTestsNeoForge() {
    }
}
