package li.cil.oc2.api.bus.device.rpc;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;

import java.util.List;

/**
 * Provides an interface for an RPC device, describing the methods that can be
 * called on it and the type names it can be detected by/is compatible with.
 * <p>
 * A {@link RPCDevice} may represent a single view onto some device or be a
 * collection of multiple aggregated {@link RPCDevice}s. One underlying device
 * may have multiple {@link RPCDevice}s providing different methods for the
 * device. This allows specifying general purpose interfaces which provide logic
 * for some aspect of an underlying device which may be shared with other devices.
 * <p>
 * The easiest and hence recommended way of implementing this interface is to use
 * the {@link ObjectDevice} class.
 *
 * @see ObjectDevice
 * @see li.cil.oc2.api.bus.device.provider.BlockDeviceProvider
 * @see li.cil.oc2.api.bus.device.provider.ItemDeviceProvider
 */
public interface RPCDevice extends Device {
    /**
     * A list of type names identifying this interface.
     * <p>
     * Device interfaces may be identified by multiple type names. Although every
     * atomic implementation will usually only have one, when compounding interfaces
     * all the underlying type names can thus be retained.
     * <p>
     * In a more general sense, these can be considered tags the device can be
     * referenced by inside a VM.
     *
     * @return the list of type names.
     */
    List<String> getTypeNames();

    /**
     * The list of methods provided by this interface.
     *
     * @return the list of methods.
     */
    List<RPCMethod> getMethods();

    /**
     * Called when the device is suspended.
     * <p>
     * This can happen when the world area containing the context the device was loaded in is unloaded,
     * e.g. due to player moving too far away from the area.
     * <p>
     * Intended for soft-releasing unmanaged resource, i.e. non-persisted unmanaged resources.
     */
    default void suspend() {
    }
}
