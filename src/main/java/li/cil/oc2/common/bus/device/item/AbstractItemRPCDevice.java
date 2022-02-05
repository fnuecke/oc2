/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.DeviceContainer;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractItemRPCDevice extends IdentityProxy<ItemStack> implements RPCDevice, ItemDevice {
    private final ObjectDevice device;
    private DeviceContainer container;

    ///////////////////////////////////////////////////////////////////

    protected AbstractItemRPCDevice(final ItemStack identity, final String typeName) {
        super(identity);
        this.device = new ObjectDevice(this, typeName);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void setDeviceContainer(@Nullable final DeviceContainer container) {
        this.container = container;
    }

    @Override
    public List<String> getTypeNames() {
        return device.getTypeNames();
    }

    @Override
    public List<RPCMethodGroup> getMethodGroups() {
        return device.getMethodGroups();
    }

    ///////////////////////////////////////////////////////////////////

    protected void setChanged() {
        if (container != null) {
            container.setChanged();
        }
    }
}
