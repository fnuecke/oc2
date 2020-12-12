package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.DeviceBusController;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptySet;

public final class DeviceBusControllerCapability {
    public static void register() {
        CapabilityManager.INSTANCE.register(DeviceBusController.class, new NullStorage<>(), Implementation::new);
    }

    ///////////////////////////////////////////////////////////////////

    private static class Implementation implements DeviceBusController {
        @Override
        public void scheduleBusScan() {
        }

        @Override
        public void scanDevices() {
        }

        @Override
        public Set<Device> getDevices() {
            return emptySet();
        }

        @Override
        public Set<UUID> getDeviceIdentifiers(final Device device) {
            return emptySet();
        }
    }
}
