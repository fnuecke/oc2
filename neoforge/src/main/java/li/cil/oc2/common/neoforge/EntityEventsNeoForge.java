/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.CatSitOnDeviceGoal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class EntityEventsNeoForge {
    @SubscribeEvent
    public static void handleEntityJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        CatSitOnDeviceGoal.onEntityLoad(event.getEntity());
    }

    private EntityEventsNeoForge() {
    }
}
