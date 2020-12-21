package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractDeviceBusElement implements DeviceBusElement {
    protected final List<Device> devices = new ArrayList<>();
    protected final HashSet<DeviceBusController> controllers = new HashSet<>();

    ///////////////////////////////////////////////////////////////////

    public void addController(final DeviceBusController controller) {
        controllers.add(controller);
    }

    @Override
    public void removeController(final DeviceBusController controller) {
        controllers.remove(controller);
    }

    @Override
    public Collection<DeviceBusController> getControllers() {
        return controllers;
    }

    @Override
    public Optional<Collection<DeviceBusElement>> getNeighbors() {
        return Optional.of(Collections.emptyList());
    }

    @Override
    public Collection<Device> getLocalDevices() {
        return devices;
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        return Optional.empty();
    }

    @Override
    public void addDevice(final Device device) {
        devices.add(device);
        scanDevices();
    }

    @Override
    public void removeDevice(final Device device) {
        devices.remove(device);
        scanDevices();
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
        for (final DeviceBusController controller : controllers) {
            controller.scheduleBusScan();
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected void scanDevices() {
        for (final DeviceBusController controller : controllers) {
            controller.scanDevices();
        }
    }
}
