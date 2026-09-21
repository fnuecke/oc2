/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.object;

import li.cil.oc2.api.bus.device.rpc.RPCBusContext;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * This interface is used to receive life-cycle callbacks on targets of an {@link ObjectDevice}.
 * <p>
 * In particular, this also includes {@link BlockEntity}s and {@link Entity}s providing {@link Callback}s.
 * <p>
 * A target may be mounted more than once at a time, e.g. once per computer it is reachable
 * from, with a distinct {@link RPCBusContext} each time. Keep the contexts in a {@code Set} to
 * send events.
 */
public interface LifecycleAwareDevice {
    /**
     * This method corresponds to {@link RPCDevice#mount(RPCBusContext)}.
     */
    default void onDeviceMounted(final RPCBusContext context) {
    }

    /**
     * This method corresponds to {@link RPCDevice#unmount(RPCBusContext)}.
     */
    default void onDeviceUnmounted(final RPCBusContext context) {
    }

    /**
     * This method corresponds to {@link RPCDevice#dispose()}.
     */
    default void onDeviceDisposed() {
    }
}
