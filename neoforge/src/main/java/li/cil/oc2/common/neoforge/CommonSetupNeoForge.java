/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.common.network.Network;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class CommonSetupNeoForge {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLCommonSetupEvent event) {
        // Architectury NetworkManager isn't synchronized, this can run in parallel, so enqueue it.
        event.enqueueWork(Network::initialize);
    }

    private CommonSetupNeoForge() {
    }
}
