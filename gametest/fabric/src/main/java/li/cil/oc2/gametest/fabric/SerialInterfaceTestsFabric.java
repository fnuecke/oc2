/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.SerialInterfaceTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class SerialInterfaceTestsFabric {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void connectorResolvesSerialCard(final GameTestHelper helper) {
        SerialInterfaceTests.connectorResolvesSerialCard(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void disabledSideExposesNoInterface(final GameTestHelper helper) {
        SerialInterfaceTests.disabledSideExposesNoInterface(helper);
    }
}
