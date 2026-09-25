/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fabric;

import li.cil.oc2.common.entity.CatSitOnDeviceGoal;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

public final class EntityEventsFabric {
    public static void initialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) ->
            CatSitOnDeviceGoal.onEntityLoad(entity));
    }

    private EntityEventsFabric() {
    }
}
