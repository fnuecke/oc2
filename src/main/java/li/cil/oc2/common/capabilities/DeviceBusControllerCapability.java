package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.DeviceBusController;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

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
        public Set<Device> getDevices() {
            return Collections.emptySet();
        }

        @Override
        public Set<UUID> getDeviceIdentifiers(final Device device) {
            return Collections.emptySet();
        }
    }
}
