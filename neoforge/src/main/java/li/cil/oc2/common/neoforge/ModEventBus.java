/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

public final class ModEventBus {
    public static IEventBus INSTANCE;
    public static ModContainer MOD_CONTAINER;

    private ModEventBus() {
    }
}
