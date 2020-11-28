package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.device.Device;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DeviceBusElementImpl implements DeviceBusElement {
    private final List<Device> devices = new ArrayList<>();
    @Nullable private DeviceBusController controller;

    @Override
    public Optional<DeviceBusController> getController() {
        return Optional.ofNullable(controller);
    }

    public void setController(@Nullable final DeviceBusController controller) {
        this.controller = controller;
    }

    @Override
    public Collection<Device> getLocalDevices() {
        return devices;
    }

    @Override
    public void addDevice(final Device device) {
        devices.add(device);
        if (controller != null) {
            controller.scanDevices();
        }
    }

    @Override
    public void removeDevice(final Device device) {
        devices.remove(device);
        if (controller != null) {
            controller.scanDevices();
        }
    }

    @Override
    public Collection<Device> getDevices() {
        if (controller != null) {
            return controller.getDevices();
        } else {
            return getLocalDevices();
        }
    }

    @Override
    public void scheduleScan() {
        if (controller != null) {
            controller.scheduleBusScan();
        }
    }
}
