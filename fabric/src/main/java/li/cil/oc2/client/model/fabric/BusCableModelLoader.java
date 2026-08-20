/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.fabric;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

public final class BusCableModelLoader {
    public static void initialize() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(BusCableModel.BUS_CABLE_STRAIGHT_MODEL, BusCableModel.BUS_CABLE_SUPPORT_MODEL);

            pluginContext.modifyModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                if (model == null || !BusCableModel.BUS_CABLE_BASE_MODEL.equals(context.resourceId())) {
                    return model;
                }

                return BusCableModel.bake(model, context.baker(), context.settings());
            });
        });
    }

    private BusCableModelLoader() {
    }
}
