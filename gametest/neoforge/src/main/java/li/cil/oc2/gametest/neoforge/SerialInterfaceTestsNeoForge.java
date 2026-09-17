/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.SerialInterfaceTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class SerialInterfaceTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aConnectorResolvesTheSerialCardOfTheComputerItIsOn(final GameTestHelper helper) {
        SerialInterfaceTests.aConnectorResolvesTheSerialCardOfTheComputerItIsOn(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aDisabledSideCarriesNoSerialTraffic(final GameTestHelper helper) {
        SerialInterfaceTests.aDisabledSideCarriesNoSerialTraffic(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void theCardHearsTheSegmentItIsOn(final GameTestHelper helper) {
        SerialInterfaceTests.theCardHearsTheSegmentItIsOn(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void twoSerialCardsFitOneComputer(final GameTestHelper helper) {
        SerialInterfaceTests.twoSerialCardsFitOneComputer(helper);
    }
}
