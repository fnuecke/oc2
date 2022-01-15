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
     * This method corresponds to {@link RPCDevice#mount()}. It is called when the device is initialized, either
     * because its virtual machine starts running, or because it is added to a running virtual machine.
     */
    default void onDeviceMounted() {
    }

    /**
     * This method corresponds to {@link RPCDevice#unmount()}. It is called when the device is disposed, either
     * because its virtual machine stops running, or because it is removed from a running virtual machine.
     */
    default void onDeviceUnmounted() {
    }

    /**
     * This method corresponds to {@link RPCDevice#suspend()}. It is called when its virtual machine is suspended,
     * either due to the containing chunk being unloaded, or the containing world being unloaded.
     */
    default void onDeviceSuspended() {
    }
}
