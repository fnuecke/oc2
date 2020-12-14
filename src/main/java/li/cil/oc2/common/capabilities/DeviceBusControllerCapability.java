package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.Objects;

public final class DeviceBusControllerCapability {
    public static void register() {
        CapabilityManager.INSTANCE.register(DeviceBusController.class, new NullStorage<>(), Implementation::new);
    }

    ///////////////////////////////////////////////////////////////////

    private static class Implementation extends AbstractDeviceBusController {
        protected Implementation() {
            super(Objects.requireNonNull(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY.getDefaultInstance()));
        }
    }
}
