/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.SerialInterfaceTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "serial_interface")
public final class SerialInterfaceTestsNeoForge {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void connectorResolvesSerialCard(final GameTestHelper helper) {
        SerialInterfaceTests.connectorResolvesSerialCard(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void disabledSideExposesNoInterface(final GameTestHelper helper) {
        SerialInterfaceTests.disabledSideExposesNoInterface(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void cardDoesNotEchoItsOwnFrame(final GameTestHelper helper) {
        SerialInterfaceTests.cardDoesNotEchoItsOwnFrame(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void twoSerialCardsFitOneComputer(final GameTestHelper helper) {
        SerialInterfaceTests.twoSerialCardsFitOneComputer(helper);
    }

    // --------------------------------------------------------------------- //

    private SerialInterfaceTestsNeoForge() {
    }
}
