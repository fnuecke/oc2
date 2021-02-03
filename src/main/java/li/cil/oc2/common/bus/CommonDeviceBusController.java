package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.Event;
import li.cil.oc2.common.util.ParameterizedEvent;
import net.minecraftforge.common.util.LazyOptional;

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
    private static final int INCOMPLETE_RETRY_INTERVAL = 10 * Constants.TICK_SECONDS;
    private static final int BAD_CONFIGURATION_RETRY_INTERVAL = 5 * Constants.TICK_SECONDS;

    ///////////////////////////////////////////////////////////////////

    public final Event onAfterBusScan = new Event();
    public final Event onBeforeScan = new Event();
    public final ParameterizedEvent<AfterDeviceScanEvent> onAfterDeviceScan = new ParameterizedEvent<>();
    public final ParameterizedEvent<DevicesChangedEvent> onDevicesAdded = new ParameterizedEvent<>();
    public final ParameterizedEvent<DevicesChangedEvent> onDevicesRemoved = new ParameterizedEvent<>();

    private final DeviceBusElement root;

    private final Set<DeviceBusElement> elements = new HashSet<>();
    private final HashSet<Device> devices = new HashSet<>();
    private final HashMap<Device, Set<UUID>> deviceIds = new HashMap<>();

    private BusState state = BusState.SCAN_PENDING;
    private int scanDelay;

    ///////////////////////////////////////////////////////////////////

    public CommonDeviceBusController(final DeviceBusElement root) {
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
        scanDelay = 0; // scan as soon as possible
        state = BusState.SCAN_PENDING;
    }

    @Override
    public void scanDevices() {
        onBeforeScan();

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
        // can detect us in the meantime (for multiple controller detection).
        clearElements();

        final HashSet<DeviceBusElement> closed = new HashSet<>();
        final Stack<DeviceBusElement> open = new Stack<>();
        final ArrayList<LazyOptional<DeviceBusElement>> optionals = new ArrayList<>();

        closed.add(root);
        open.add(root);

        while (!open.isEmpty()) {
            final DeviceBusElement element = open.pop();

            final Optional<Collection<LazyOptional<DeviceBusElement>>> elementNeighbors = element.getNeighbors();
            if (!elementNeighbors.isPresent()) {
                scanDelay = INCOMPLETE_RETRY_INTERVAL;
                state = BusState.INCOMPLETE;
                return;
            }

            elementNeighbors.ifPresent(neighbors -> {
                for (final LazyOptional<DeviceBusElement> neighbor : neighbors) {
                    neighbor.ifPresent(neighborElement -> {
                        if (closed.add(neighborElement)) {
                            open.add(neighborElement);
                            optionals.add(neighbor);
                        }
                    });
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

        // Rescan if any bus element gets invalidated.
        for (final LazyOptional<DeviceBusElement> optional : optionals) {
            assert optional.isPresent();
            optional.addListener(unused -> scheduleBusScan());
        }

        onAfterBusScan();

        scanDevices();

        state = BusState.READY;
    }

    ///////////////////////////////////////////////////////////////////

    protected Collection<DeviceBusElement> getElements() {
        return elements;
    }

    protected void onAfterBusScan() {
        onAfterBusScan.run();
    }

    protected void onBeforeScan() {
        onBeforeScan.run();
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
    }

    ///////////////////////////////////////////////////////////////////

    public static final class AfterDeviceScanEvent {
        public final boolean didDevicesChange;

        public AfterDeviceScanEvent(final boolean didDevicesChange) {
            this.didDevicesChange = didDevicesChange;
        }
    }

    public static final class DevicesChangedEvent {
        public final Collection<Device> devices;

        public DevicesChangedEvent(final Collection<Device> devices) {
            this.devices = devices;
        }
    }
}
