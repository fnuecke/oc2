/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.fabric;

import li.cil.oc2.instrumentation.Instrumentation;
import net.fabricmc.api.ModInitializer;

public final class InstrumentationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Instrumentation.initialize();
    }
}
