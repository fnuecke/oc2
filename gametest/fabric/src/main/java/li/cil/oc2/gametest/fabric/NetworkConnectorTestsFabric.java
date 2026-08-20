/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.NetworkConnectorTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.FabricTestSupport.TEMPLATE;

public final class NetworkConnectorTestsFabric {
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public void networkCableLinksConnectorsWhenUsed(final GameTestHelper helper) {
        NetworkConnectorTests.networkCableLinksConnectorsWhenUsed(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void aConnectorResolvesTheNetworkCardOfTheComputerItIsOn(final GameTestHelper helper) {
        NetworkConnectorTests.aConnectorResolvesTheNetworkCardOfTheComputerItIsOn(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void aConnectorNoticesTheNetworkCardBeingRemoved(final GameTestHelper helper) {
        NetworkConnectorTests.aConnectorNoticesTheNetworkCardBeingRemoved(helper);
    }
}
