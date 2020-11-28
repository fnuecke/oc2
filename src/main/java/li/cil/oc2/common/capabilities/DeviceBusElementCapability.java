package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.vm.DeviceBusElementImpl;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class DeviceBusElementCapability {
    public static void register() {
        CapabilityManager.INSTANCE.register(DeviceBusElement.class, new NullStorage<>(), DeviceBusElementImpl::new);
    }
}
