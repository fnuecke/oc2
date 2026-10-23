/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.NetworkConnectorTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class NetworkConnectorTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 300)
    public void removingAConnectorUnlinksItsPeer(final GameTestHelper helper) {
        NetworkConnectorTests.removingAConnectorUnlinksItsPeer(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 300)
    public void silentlyRemovedConnectorKeepsItsPeer(final GameTestHelper helper) {
        NetworkConnectorTests.silentlyRemovedConnectorKeepsItsPeer(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 300)
    public void stateChangeKeepsTheLink(final GameTestHelper helper) {
        NetworkConnectorTests.stateChangeKeepsTheLink(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 300)
    public void networkCableLinksConnectorsWhenUsed(final GameTestHelper helper) {
        NetworkConnectorTests.networkCableLinksConnectorsWhenUsed(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 600)
    public void connectorResolvesNetworkCard(final GameTestHelper helper) {
        NetworkConnectorTests.connectorResolvesNetworkCard(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 600)
    public void connectorDropsRemovedCard(final GameTestHelper helper) {
        NetworkConnectorTests.connectorDropsRemovedCard(helper);
    }
}
