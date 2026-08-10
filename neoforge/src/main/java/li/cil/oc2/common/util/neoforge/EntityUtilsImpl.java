/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util.neoforge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class EntityUtilsImpl {
    public static CompoundTag getPersistentData(final Entity entity) {
        return entity.getPersistentData();
    }

    private EntityUtilsImpl() {
    }
}
