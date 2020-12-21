package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;

import java.util.*;

import static java.util.Collections.emptySet;

public abstract class AbstractDeviceBusController implements DeviceBusController {
    public enum BusState {
        SCAN_PENDING,
        INCOMPLETE,
        TOO_COMPLEX,
        MULTIPLE_CONTROLLERS,
        READY,
    }

    ///////////////////////////////////////////////////////////////////

    private static final int MAX_BUS_ELEMENT_COUNT = 128;
    private static final int TICKS_PER_SECOND = 20;
    private static final int INCOMPLETE_RETRY_INTERVAL = 10 * TICKS_PER_SECOND;
    private static final int BAD_CONFIGURATION_RETRY_INTERVAL = 5 * TICKS_PER_SECOND;

    ///////////////////////////////////////////////////////////////////

    private final DeviceBusElement root;

    private final Set<DeviceBusElement> elements = new HashSet<>();
    private final HashSet<Device> devices = new HashSet<>();
    private final HashMap<Device, Set<UUID>> deviceIds = new HashMap<>();

    private BusState state = BusState.SCAN_PENDING;
    private int scanDelay;

    ///////////////////////////////////////////////////////////////////

    protected AbstractDeviceBusController(final DeviceBusElement root) {
        this.root = root;
    }

    ///////////////////////////////////////////////////////////////////

    public void dispose() {
        for (final DeviceBusElement element : elements) {
            element.removeController(this);
            for (final DeviceBusController controller : element.getControllers()) {
                controller.scheduleBusScan();
            }
        }

        elements.clear();
    }

    public BusState getState() {
        return state;
    }

    @Override
    public void scheduleBusScan() {
        onDevicesInvalid();

        scanDelay = 0; // scan as soon as possible
        state = BusState.SCAN_PENDING;
    }

    @Override
    public void scanDevices() {
        onDevicesInvalid();

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

        onDevicesValid(didDevicesChange || didDeviceIdsChange);
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
        // can detect us in the meantime (for multiple controller detection).
        clearElements();

        final HashSet<DeviceBusElement> closed = new HashSet<>();
        final Stack<DeviceBusElement> open = new Stack<>();

        closed.add(root);
        open.add(root);

        while (!open.isEmpty()) {
            final DeviceBusElement element = open.pop();

            final Optional<Collection<DeviceBusElement>> elementNeighbors = element.getNeighbors();
            if (!elementNeighbors.isPresent()) {
                scanDelay = INCOMPLETE_RETRY_INTERVAL;
                state = BusState.INCOMPLETE;
                return;
            }

            elementNeighbors.ifPresent(neighbors -> {
                for (final DeviceBusElement neighbor : neighbors) {
                    if (closed.add(neighbor)) {
                        open.add(neighbor);
                    }
                }
            });

            if (closed.size() > MAX_BUS_ELEMENT_COUNT) {
                scanDelay = BAD_CONFIGURATION_RETRY_INTERVAL;
                state = BusState.TOO_COMPLEX;
                return;
            }
        }

        final HashSet<DeviceBusController> controllers = new HashSet<>();
        for (final DeviceBusElement element : closed) {
            controllers.addAll(element.getControllers());
            element.addController(this);
        }

        controllers.remove(this); // Just in case...
        elements.addAll(closed);

        // If there's any controllers on the bus that are not us, enter error state and
        // trigger a scan for those controllers, too, so they may enter error state.
        if (!controllers.isEmpty()) {
            for (final DeviceBusController controller : controllers) {
                controller.scheduleBusScan();
            }

            state = BusState.MULTIPLE_CONTROLLERS;
            scanDelay = BAD_CONFIGURATION_RETRY_INTERVAL;
            return;
        }

        scanDevices();

        state = BusState.READY;
    }

    ///////////////////////////////////////////////////////////////////

    protected void onDevicesInvalid() {
    }

    protected void onDevicesValid(final boolean didDevicesChange) {
    }

    protected void onDevicesAdded(final Set<Device> devices) {
    }

    protected void onDevicesRemoved(final Set<Device> devices) {
    }

    ///////////////////////////////////////////////////////////////////

    private void clearElements() {
        for (final DeviceBusElement element : elements) {
            element.removeController(this);
        }

        elements.clear();
    }
}
