package li.cil.oc2.api.device.object;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import java.util.Collection;

/**
 * This interface is used to declare additional type names for a device on targets of an {@link ObjectDeviceInterface}.
 * <p>
 * In particular {@link Block}s and {@link TileEntity}s that contain {@link Callback} methods may implement
 * this interface to provide additional type names.
 */
public interface NamedDevice {
    /**
     * A list of additional type names to associate with any {@link ObjectDeviceInterface} used
     * to make available methods in an instance of a class implementing this interface.
     *
     * @return the list of additional type names.
     */
    Collection<String> getDeviceTypeNames();
}
