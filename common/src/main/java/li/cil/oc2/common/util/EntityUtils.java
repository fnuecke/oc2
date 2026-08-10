/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class EntityUtils {
    @ExpectPlatform
    public static CompoundTag getPersistentData(final Entity entity) {
        throw new AssertionError();
    }

    private EntityUtils() {
    }
}
