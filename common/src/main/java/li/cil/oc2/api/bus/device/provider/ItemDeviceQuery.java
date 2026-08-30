/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.provider;

import li.cil.oc2.api.bus.device.vm.ArchitectureType;
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
