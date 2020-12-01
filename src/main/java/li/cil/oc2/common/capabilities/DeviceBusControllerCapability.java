package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.device.Device;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.Collection;
import java.util.Collections;

public final class DeviceBusControllerCapability {
    public static void register() {
        CapabilityManager.INSTANCE.register(DeviceBusController.class, new NullStorage<>(), Implementation::new);
    }

    private static class Implementation implements DeviceBusController {
        @Override
        public void scheduleBusScan() {
        }

        @Override
        public void scanDevices() {
        }

        @Override
        public Collection<Device> getDevices() {
            return Collections.emptyList();
        }
    }
}
