/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.fabric;

import li.cil.manual.api.platform.FabricManualInitializer;
import li.cil.oc2.client.manual.Manuals;

public final class ManualInitializer implements FabricManualInitializer {
    @Override
    public void registerManualObjects() {
        Manuals.initialize();
    }
}
