/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import net.minecraftforge.common.util.LazyOptional;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractDeviceBusElement implements DeviceBusElement {
    protected final Object2IntArrayMap<Device> devices = new Object2IntArrayMap<>();
    protected final HashSet<DeviceBusController> controllers = new HashSet<>();

    ///////////////////////////////////////////////////////////////////

    public void addDevice(final Device device) {
        devices.put(device, 0);
        scanDevices();
    }

    @Override
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
    public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
        return Optional.of(Collections.emptyList());
    }

    @Override
    public Collection<Device> getLocalDevices() {
        return devices.keySet();
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        return Optional.empty();
    }

    @Override
    public double getEnergyConsumption() {
        long accumulator = 0;
        for (final Object2IntMap.Entry<Device> entry : devices.object2IntEntrySet()) {
            accumulator += entry.getIntValue();
        }
        return accumulator;
    }

    @Override
    public Collection<Device> getDevices() {
        if (!controllers.isEmpty()) {
            return controllers.stream().flatMap(controller -> getDevices().stream()).collect(Collectors.toSet());
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
