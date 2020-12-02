package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.device.Device;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class DeviceBusElementCapability {
    public static void register() {
        CapabilityManager.INSTANCE.register(DeviceBusElement.class, new NullStorage<>(), Implementation::new);
    }

    public static final class Implementation implements DeviceBusElement {
        private final List<Device> devices = new ArrayList<>();
        private final HashSet<DeviceBusController> controllers = new HashSet<>();

        public void addController(final DeviceBusController controller) {
            controllers.add(controller);
        }

        @Override
        public void removeController(final DeviceBusController controller) {
            controllers.remove(controller);
        }

        @Override
        public Collection<Device> getLocalDevices() {
            return devices;
        }

        @Override
        public void addDevice(final Device device) {
            devices.add(device);
            for (final DeviceBusController controller : controllers) {
                controller.scanDevices();
            }
        }

        @Override
        public void removeDevice(final Device device) {
            devices.remove(device);
            for (final DeviceBusController controller : controllers) {
                controller.scanDevices();
            }
        }

        @Override
        public Collection<Device> getDevices() {
            if (!controllers.isEmpty()) {
                return controllers.stream().flatMap(controller -> getDevices().stream()).collect(Collectors.toList());
            } else {
                return getLocalDevices();
            }
        }

        @Override
        public void scheduleScan() {
            // Controllers are expected to remove themselves when a scan is scheduled.
            final ArrayList<DeviceBusController> oldControllers = new ArrayList<>(controllers);
            for (final DeviceBusController controller : oldControllers) {
                controller.scheduleBusScan();
            }
            assert controllers.isEmpty();
        }
    }
}
