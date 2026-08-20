/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.api.platform.FabricRegistrationInitializer;
import li.cil.oc2.gametest.device.GuestTestDevices;

public final class GameTestsFabric implements FabricRegistrationInitializer {
    @Override
    public void registerObjects() {
        GuestTestDevices.initialize();
    }
}
