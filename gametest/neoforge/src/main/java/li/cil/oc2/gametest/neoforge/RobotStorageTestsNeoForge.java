/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.RobotStorageTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.BOOT_TIMEOUT_TICKS;
import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotStorageTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_robot_storage")
    public static void destroyedRobotReleasesItsBlobs(final GameTestHelper helper) {
        RobotStorageTests.destroyedRobotReleasesItsBlobs(helper);
    }

    // --------------------------------------------------------------------- //

    private RobotStorageTestsNeoForge() {
    }
}
