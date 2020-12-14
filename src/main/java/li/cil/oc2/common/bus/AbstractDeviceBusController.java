package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import net.minecraftforge.common.util.LazyOptional;

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

    public BusState getState() {
        return state;
    }

    @Override
    public void scheduleBusScan() {
        if (scanDelay >= 0) {
            return; // Scan already pending.
        }

        onDevicesInvalid();

        for (final DeviceBusElement element : elements) {
            element.getController()
                    .filter(c -> c == this)
                    .ifPresent(c -> element.setController (null));
        }

        elements.clear();
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
        assert elements.isEmpty();

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

        // Collect all controllers connected to this bus. While every bus element
        // should have the same controller, let's play it safe and explicitly
        // collect all controllers from all elements for this check.
        final HashSet<DeviceBusController> controllers = new HashSet<>();
        for (final DeviceBusElement element : closed) {
            element.getController().ifPresent(controllers::add);
        }

        // This controller should not possibly be in the list of any bus element, but
        // again, let's rather be paranoid.
        controllers.remove(this);

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

        for (final DeviceBusElement element : closed) {
            element.setController(this);
        }

        elements.addAll(closed);

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
}
