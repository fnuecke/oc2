package li.cil.oc2.api.bus.device.provider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Device query for a block in the world.
 *
 * @see BlockDeviceProvider
 */
public interface BlockDeviceQuery {
    /**
     * The world containing the block this query is performed for.
     *
     * @return the world containing the block.
     */
    Level getLevel();

    /**
     * The position of the block this query is performed for.
     *
     * @return the position of the block.
     */
    BlockPos getQueryPosition();

    /**
     * The side of the block this query is performed on, if any.
     * <p>
     * May be {@code null} just as when requesting a capability from a tile entity.
     *
     * @return the side of the block.
     */
    @Nullable
    Direction getQuerySide();
}
