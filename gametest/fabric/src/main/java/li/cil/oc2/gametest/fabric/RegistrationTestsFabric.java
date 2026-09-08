/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.RegistrationTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class RegistrationTestsFabric {
    @GameTest(template = TEMPLATE)
    public void deviceTypesAreRegistered(final GameTestHelper helper) {
        RegistrationTests.deviceTypesAreRegistered(helper);
    }
}
