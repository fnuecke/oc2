/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

/**
 * Implementing this interface allows providing positional information to the {@link DeviceBusController}.
 * <p>
 * Controllers may use this information to automatically trigger bus scans when the chunk containing
 * this element or a chunk adjacent to it gets unloaded / loaded. This convenience allows not having
 * to implement logic in bus element implementations to trigger such scans themselves.
 */
public interface BlockDeviceBusElement extends DeviceBusElement {
    /**
     * The level the bus lives in.
     *
     * @return the level the bus lives in.
     */
    @Nullable
    LevelAccessor getLevel();

    /**
     * The position of this bus element.
     *
     * @return the position of this bus element.
     */
    BlockPos getPosition();
}
