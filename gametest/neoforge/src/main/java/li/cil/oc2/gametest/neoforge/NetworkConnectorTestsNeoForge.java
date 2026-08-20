/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.NetworkConnectorTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkConnectorTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void connectorsLinkWithClearLineOfSight(final GameTestHelper helper) {
        NetworkConnectorTests.connectorsLinkWithClearLineOfSight(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void connectorsRefuseObstructedLink(final GameTestHelper helper) {
        NetworkConnectorTests.connectorsRefuseObstructedLink(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void connectorsRefuseLinkBeyondRange(final GameTestHelper helper) {
        NetworkConnectorTests.connectorsRefuseLinkBeyondRange(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void networkCableLinksConnectorsWhenUsed(final GameTestHelper helper) {
        NetworkConnectorTests.networkCableLinksConnectorsWhenUsed(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aConnectorResolvesTheNetworkCardOfTheComputerItIsOn(final GameTestHelper helper) {
        NetworkConnectorTests.aConnectorResolvesTheNetworkCardOfTheComputerItIsOn(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aConnectorNoticesTheNetworkCardBeingRemoved(final GameTestHelper helper) {
        NetworkConnectorTests.aConnectorNoticesTheNetworkCardBeingRemoved(helper);
    }

    // ------------------------------------------------------------- //

    private NetworkConnectorTestsNeoForge() {
    }
}
