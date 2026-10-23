/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.fabric;

import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;

public final class BusCableModelLoader {
    public static void initialize() {
        CustomUnbakedBlockStateModel.register(BusCableModel.ID, BusCableModel.CODEC);
    }

    private BusCableModelLoader() {
    }
}
