/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.provider;

import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Device query for a block in the world.
 *
 * @see BlockDeviceProvider
 */
public interface BlockDeviceQuery {
    /**
     * The architecture of the machine this query is for.
     * <p>
     * Allows providers to select a different device implementation based on the architecture.
     * This can be useful when different architectures are just so fundamentally different that
     * the same device simply cannot work on both as-is, but from a gameplay perspective we still
     * want to provide a seamless experience that "just works".
     * <p>
     * In some cases, e.g. for tooltip queries, this is left empty.
     *
     * @return the architecture the device would be built for, if any.
     */
    Optional<ArchitectureType> getArchitectureType();

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
