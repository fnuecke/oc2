/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.GameTestReporting;
import li.cil.oc2.gametest.device.GuestTestDevices;
import net.neoforged.fml.common.Mod;

import java.io.File;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;

@Mod(MOD_ID)
public final class GameTests {
    private static final String JUNIT_OUTPUT_DIR_PROPERTY = "oc2.gameTest.junitDir";
    private static final String REPORT_FILE_NAME = "neoforge-game-tests.xml";

    public GameTests() {
        GuestTestDevices.initialize();

        final String directory = System.getProperty(JUNIT_OUTPUT_DIR_PROPERTY);
        GameTestReporting.install(directory == null || directory.isEmpty()
            ? null
            : new File(directory, REPORT_FILE_NAME));
    }
}
