/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fabric;

import li.cil.oc2.common.entity.CatBehaviorGoal;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

public final class EntityEventsFabric {
    public static void initialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) ->
            CatBehaviorGoal.onEntityLoad(entity));
    }

    private EntityEventsFabric() {
    }
}
