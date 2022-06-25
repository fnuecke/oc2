/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.provider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * Device query for an item stack.
 *
 * @see ItemDeviceProvider
 */
public interface ItemDeviceQuery {
    /**
     * The {@link BlockEntity} that holds the item this query is for.
     *
     * @return the {@link BlockEntity} hosting the device, if any.
     */
    Optional<BlockEntity> getContainerBlockEntity();

    /**
     * The {@link Entity} that holds the item this query is for.
     *
     * @return the {@link Entity} hosting the device, if any.
     */
    Optional<Entity> getContainerEntity();

    /**
     * The item stack this query is performed for.
     *
     * @return the item stack to get a device for.
     */
    ItemStack getItemStack();
}
