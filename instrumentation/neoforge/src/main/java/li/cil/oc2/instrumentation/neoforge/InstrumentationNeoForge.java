/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.neoforge;

import li.cil.oc2.instrumentation.Instrumentation;
import net.neoforged.fml.common.Mod;

@Mod(Instrumentation.MOD_ID)
public final class InstrumentationNeoForge {
    public InstrumentationNeoForge() {
        Instrumentation.initialize();
    }
}
