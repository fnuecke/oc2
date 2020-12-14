package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractDeviceBusElement implements DeviceBusElement {
    protected final List<Device> devices = new ArrayList<>();
    protected DeviceBusController controller;

    ///////////////////////////////////////////////////////////////////

    public void setController(@Nullable final DeviceBusController controller) {
        this.controller = controller;
    }

    @Override
    public Optional<DeviceBusController> getController() {
        return Optional.ofNullable(controller);
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
        if (controller != null) {
            return controller.getDevices();
        } else {
            return getLocalDevices();
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected void scanDevices() {
        if (controller != null) {
            controller.scanDevices();
        }
    }
}
