/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.neoforge;

import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.network.Network;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CommonSetupNeoForge {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLCommonSetupEvent event) {
        Network.initialize();
        RPCMethodParameterTypeAdapters.initialize();
    }

    private CommonSetupNeoForge() {
    }
}
