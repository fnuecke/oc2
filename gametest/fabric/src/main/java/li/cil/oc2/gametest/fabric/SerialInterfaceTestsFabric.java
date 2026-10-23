/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.SerialInterfaceTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class SerialInterfaceTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 600)
    public void connectorResolvesSerialCard(final GameTestHelper helper) {
        SerialInterfaceTests.connectorResolvesSerialCard(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 600)
    public void disabledSideExposesNoInterface(final GameTestHelper helper) {
        SerialInterfaceTests.disabledSideExposesNoInterface(helper);
    }
}
