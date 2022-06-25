/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.provider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Device query for a block in the world.
 *
 * @see BlockDeviceProvider
 */
public interface BlockDeviceQuery {
    /**
     * The level containing the block this query is performed for.
     *
     * @return the level containing the block.
     */
    LevelAccessor getLevel();

    /**
     * The position of the block this query is performed for.
     *
     * @return the position of the block.
     */
    BlockPos getQueryPosition();

    /**
     * The world-space side of the block this query is performed on, if any.
     * <p>
     * May be {@code null} just as when requesting a capability from a {@link BlockEntity}.
     *
     * @return the side of the block.
     */
    @Nullable
    Direction getQuerySide();
}
