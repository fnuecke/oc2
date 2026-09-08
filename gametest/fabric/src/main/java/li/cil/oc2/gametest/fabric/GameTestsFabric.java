/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.api.platform.FabricRegistrationInitializer;
import li.cil.oc2.gametest.device.GuestTestDevices;
import li.cil.oc2.gametest.util.GameTestReporting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.io.File;

public final class GameTestsFabric implements FabricRegistrationInitializer {
    private static final String REPORT_FILE_PROPERTY = "fabric-api.gametest.report-file";

    @Override
    public void registerObjects() {
        GuestTestDevices.initialize();

        // Make sure our reporter wins.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            final String report = System.getProperty(REPORT_FILE_PROPERTY);
            GameTestReporting.install(report == null || report.isEmpty() ? null : new File(report));
        });
    }
}
