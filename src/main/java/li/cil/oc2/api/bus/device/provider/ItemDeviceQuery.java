package li.cil.oc2.api.bus.device.provider;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.util.Optional;

/**
 * Device query for an item stack.
 *
 * @see ItemDeviceProvider
 */
public interface ItemDeviceQuery {
    /**
     * The {@link TileEntity} that holds the item this query is for.
     *
     * @return the {@link TileEntity} hosting the device, if any.
     */
    Optional<TileEntity> getContainerTileEntity();

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
