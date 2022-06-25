/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.util.Event;
import li.cil.oc2.common.util.LazyOptionalUtils;
import li.cil.oc2.common.util.ParameterizedEvent;
import li.cil.oc2.common.util.TickUtils;
import net.minecraftforge.common.util.LazyOptional;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptySet;

public class CommonDeviceBusController implements DeviceBusController {
    public enum BusState {
        SCAN_PENDING,
        INCOMPLETE,
        TOO_COMPLEX,
        MULTIPLE_CONTROLLERS,
        READY,
    }

    ///////////////////////////////////////////////////////////////////

    private static final int MAX_BUS_ELEMENT_COUNT = 128;
    private static final int INCOMPLETE_RETRY_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(10));
    private static final int BAD_CONFIGURATION_RETRY_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(5));

    ///////////////////////////////////////////////////////////////////

    public final Event onAfterBusScan = new Event();
    public final Event onBeforeDeviceScan = new Event();
    public final ParameterizedEvent<AfterDeviceScanEvent> onAfterDeviceScan = new ParameterizedEvent<>();
    public final ParameterizedEvent<DevicesChangedEvent> onDevicesAdded = new ParameterizedEvent<>();
    public final ParameterizedEvent<DevicesChangedEvent> onDevicesRemoved = new ParameterizedEvent<>();

    private final DeviceBusElement root;
    private final int baseEnergyConsumption;

    private final Set<DeviceBusElement> elements = new HashSet<>();
    private final HashSet<Device> devices = new HashSet<>();
    private final HashMap<Device, Set<UUID>> deviceIds = new HashMap<>();

    private BusState state = BusState.SCAN_PENDING;
    private int scanDelay;

    private int energyConsumption;

    ///////////////////////////////////////////////////////////////////

    public CommonDeviceBusController(final DeviceBusElement root, final int baseEnergyConsumption) {
        this.root = root;
        this.baseEnergyConsumption = baseEnergyConsumption;
    }

    ///////////////////////////////////////////////////////////////////

    public void setDeviceContainersChanged() {
    }

    public void dispose() {
        for (final DeviceBusElement element : elements) {
            element.removeController(this);

            // Let other controllers on the bus know we're gone, so they can quickly recover.
            for (final DeviceBusController controller : element.getControllers()) {
                controller.scheduleBusScan(ScanReason.BUS_CHANGE);
            }
        }

        elements.clear();
    }

    public BusState getState() {
        return state;
    }

    public int getEnergyConsumption() {
        return energyConsumption;
    }

    @Override
    public void scheduleBusScan(final ScanReason reason) {
        // For notification of a bus error, we just keep our old state and delay, if we have one.
        // Avoids ping-ponging error states causing scans every tick.
        if (reason == ScanReason.BUS_ERROR && state.ordinal() < BusState.READY.ordinal()) {
            return;
        }

        scanDelay = 0; // scan as soon as possible
        state = BusState.SCAN_PENDING;
    }

    @Override
    public void scanDevices() {
        onBeforeDeviceScan();

        final HashSet<Device> newDevices = new HashSet<>();
        final HashMap<Device, Set<UUID>> newDeviceIds = new HashMap<>();
        for (final DeviceBusElement element : elements) {
            for (final Device device : element.getLocalDevices()) {
                newDevices.add(device);
                element.getDeviceIdentifier(device).ifPresent(identifier -> newDeviceIds
                    .computeIfAbsent(device, unused -> new HashSet<>()).add(identifier));
            }
        }

        final HashSet<Device> removedDevices = new HashSet<>(devices);
        removedDevices.removeAll(newDevices);
        onDevicesRemoved(removedDevices);

        final HashSet<Device> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(devices);
        onDevicesAdded(addedDevices);

        final boolean didDevicesChange = !removedDevices.isEmpty() || !addedDevices.isEmpty();
        final boolean didDeviceIdsChange;
        if (didDevicesChange) {
            devices.clear();
            devices.addAll(newDevices);

            didDeviceIdsChange = true;
        } else {
            didDeviceIdsChange = deviceIds.entrySet().stream().anyMatch(entry ->
                !Objects.equals(entry.getValue(), newDeviceIds.get(entry.getKey())));
        }

        if (didDeviceIdsChange) {
            deviceIds.clear();
            deviceIds.putAll(newDeviceIds);
        }

        onAfterDeviceScan(didDevicesChange || didDeviceIdsChange);
    }

    @Override
    public Set<Device> getDevices() {
        return devices;
    }

    @Override
    public Set<UUID> getDeviceIdentifiers(final Device device) {
        return deviceIds.getOrDefault(device, emptySet());
    }

    public void scan() {
        if (scanDelay < 0) {
            return;
        }

        if (scanDelay-- > 0) {
            return;
        }

        assert scanDelay == -1;

        // We stay registered with elements until we scan so that other controllers on the same bus
        // can detect us in the meantime (for multiple controller detection). This also means that
        // adapters keep devices mounted until a scan, which is a nice performance plus.

        collectBusElements().ifPresent(optionals -> {
            final HashSet<DeviceBusElement> addedElements = updateElements(optionals.keySet());

            if (checkOtherBusControllers()) {
                return;
            }

            // Don't have an optional for our root element, so skip that.
            addedElements.remove(root);
            for (final DeviceBusElement element : addedElements) {
                // Rescan if any bus element gets invalidated. Don't have bus elements keep this instance alive,
                // only notify us on change if we still exist.
                LazyOptionalUtils.addWeakListener(optionals.get(element), this,
                    (controller, ignored) -> controller.scheduleBusScan(ScanReason.BUS_CHANGE));
            }

            scanDevices();

            updateEnergyConsumption();

            state = BusState.READY;

            onAfterBusScan();
        });
    }

    ///////////////////////////////////////////////////////////////////

    protected Collection<DeviceBusElement> getElements() {
        return elements;
    }

    protected void onAfterBusScan() {
        onAfterBusScan.run();
    }

    protected void onBeforeDeviceScan() {
        onBeforeDeviceScan.run();
    }

    protected void onAfterDeviceScan(final boolean didDevicesChange) {
        onAfterDeviceScan.accept(new AfterDeviceScanEvent(didDevicesChange));
    }

    protected void onDevicesAdded(final Collection<Device> devices) {
        onDevicesAdded.accept(new DevicesChangedEvent(devices));
    }

    protected void onDevicesRemoved(final Collection<Device> devices) {
        onDevicesRemoved.accept(new DevicesChangedEvent(devices));
    }

    ///////////////////////////////////////////////////////////////////

    private void clearElements() {
        for (final DeviceBusElement element : elements) {
            element.removeController(this);
        }

        elements.clear();

        scanDevices();
    }

    private Optional<HashMap<DeviceBusElement, LazyOptional<DeviceBusElement>>> collectBusElements() {
        final HashSet<DeviceBusElement> closed = new HashSet<>();
        final Stack<DeviceBusElement> open = new Stack<>();
        final HashMap<DeviceBusElement, LazyOptional<DeviceBusElement>> optionals = new HashMap<>();

        closed.add(root);
        open.add(root);
        optionals.put(root, LazyOptional.empty()); // Needed because we only return this map.

        while (!open.isEmpty()) {
            final DeviceBusElement element = open.pop();

            final Optional<Collection<LazyOptional<DeviceBusElement>>> elementNeighbors = element.getNeighbors();
            if (elementNeighbors.isEmpty()) {
                scanDelay = INCOMPLETE_RETRY_INTERVAL;
                state = BusState.INCOMPLETE;

                clearElements();
                return Optional.empty();
            }

            for (final LazyOptional<DeviceBusElement> neighbor : elementNeighbors.get()) {
                neighbor.ifPresent(neighborElement -> {
                    if (closed.add(neighborElement)) {
                        open.add(neighborElement);
                        optionals.put(neighborElement, neighbor);
                    }
                });
            }

            if (closed.size() > MAX_BUS_ELEMENT_COUNT) {
                scanDelay = BAD_CONFIGURATION_RETRY_INTERVAL;
                state = BusState.TOO_COMPLEX;

                clearElements();
                return Optional.empty();
            }
        }

        return Optional.of(optionals);
    }

    private HashSet<DeviceBusElement> updateElements(final Set<DeviceBusElement> newElements) {
        final HashSet<DeviceBusElement> removedElements = new HashSet<>(elements);
        removedElements.removeAll(newElements);

        elements.removeAll(removedElements);

        for (final DeviceBusElement removedElement : removedElements) {
            removedElement.removeController(this);

            // Let other controllers on the bus know we're gone, so they can quickly recover.
            for (final DeviceBusController controller : removedElement.getControllers()) {
                controller.scheduleBusScan(ScanReason.BUS_CHANGE);
            }
        }

        final HashSet<DeviceBusElement> addedElements = new HashSet<>(newElements);
        addedElements.removeAll(elements);

        elements.addAll(addedElements);

        for (final DeviceBusElement element : addedElements) {
            element.addController(this);
        }
        return addedElements;
    }

    private boolean checkOtherBusControllers() {
        final HashSet<DeviceBusController> controllers = new HashSet<>();
        for (final DeviceBusElement element : elements) {
            controllers.addAll(element.getControllers());
        }

        controllers.remove(this);

        if (controllers.isEmpty()) {
            return false;
        }

        // If there's any controllers on the bus that are not this one, enter error state and
        // trigger a scan for those controllers, too, so they may enter error state.

        for (final DeviceBusController controller : controllers) {
            controller.scheduleBusScan(ScanReason.BUS_ERROR);
        }

        state = BusState.MULTIPLE_CONTROLLERS;
        scanDelay = BAD_CONFIGURATION_RETRY_INTERVAL;
        return true;
    }

    private void updateEnergyConsumption() {
        double accumulator = baseEnergyConsumption;
        for (final DeviceBusElement element : elements) {
            accumulator += Math.max(0, element.getEnergyConsumption());
        }

        if (accumulator > Integer.MAX_VALUE) {
            energyConsumption = Integer.MAX_VALUE;
        } else {
            energyConsumption = (int) Math.ceil(accumulator);
        }
    }

    ///////////////////////////////////////////////////////////////////

    public record AfterDeviceScanEvent(boolean didDevicesChange) { }

    public record DevicesChangedEvent(Collection<Device> devices) { }
}
