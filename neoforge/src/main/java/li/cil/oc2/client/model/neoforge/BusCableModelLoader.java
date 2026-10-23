/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.neoforge;

import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

public final class BusCableModelLoader {
    public static void register(final RegisterBlockStateModels event) {
        event.registerModel(BusCableModel.ID, BusCableModel.CODEC);
    }

    private BusCableModelLoader() {
    }
}
