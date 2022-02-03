/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.object;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * This interface is used to receive life-cycle callbacks on targets of an {@link ObjectDevice}.
 * <p>
 * In particular, this also includes {@link BlockEntity}s and {@link Entity}s providing {@link Callback}s.
 */
public interface LifecycleAwareDevice {
    /**
     * This method corresponds to {@link RPCDevice#mount()}.
     */
    default void onDeviceMounted() {
    }

    /**
     * This method corresponds to {@link RPCDevice#unmount()}.
     */
    default void onDeviceUnmounted() {
    }

    /**
     * This method corresponds to {@link RPCDevice#dispose()}.
     */
    default void onDeviceDisposed() {
    }
}
