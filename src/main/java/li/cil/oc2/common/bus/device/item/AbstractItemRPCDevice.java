package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import net.minecraft.item.ItemStack;

import java.util.List;

public abstract class AbstractItemRPCDevice extends IdentityProxy<ItemStack> implements RPCDevice, ItemDevice {
    private final ObjectDevice device;

    ///////////////////////////////////////////////////////////////////

    protected AbstractItemRPCDevice(final ItemStack identity, final String typeName) {
        super(identity);
        this.device = new ObjectDevice(this, typeName);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public List<String> getTypeNames() {
        return device.getTypeNames();
    }

    @Override
    public List<RPCMethod> getMethods() {
        return device.getMethods();
    }
}
