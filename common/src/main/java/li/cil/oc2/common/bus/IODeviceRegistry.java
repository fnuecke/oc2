/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IOBusContext;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.sedna.api.device.bus.DeviceDescription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class IODeviceRegistry {
    @FunctionalInterface
    interface EventSink {
        boolean sendEvent(UUID key, int code, int value);
    }

    // --------------------------------------------------------------------- //

    private static final Logger LOGGER = LogManager.getLogger(IODeviceRegistry.class);

    private static final int MAX_DEVICES = 254;

    // --------------------------------------------------------------------- //

    private final EventSink events;
    private List<Binding> bindings = List.of();
    private Map<UUID, Binding> bindingsByKey = Map.of();
    private volatile Map<UUID, Integer> indexByKey = Map.of(); // read by event senders on any thread
    private List<DeviceDescription> descriptions = List.of();
    private final Map<IODevice, MountedContext> contexts = new HashMap<>();

    // --------------------------------------------------------------------- //

    IODeviceRegistry(final EventSink events) {
        this.events = events;
    }

    // --------------------------------------------------------------------- //

    List<DeviceDescription> descriptions() {
        return descriptions;
    }

    @Nullable
    UUID keyAt(final int index) {
        return index < bindings.size() ? bindings.get(index).key() : null;
    }

    int indexOf(final UUID key) {
        return indexByKey.get(key);
    }

    boolean contains(final UUID key) {
        return indexByKey.containsKey(key);
    }

    @Nullable
    IODevice byKey(final UUID key) {
        final Binding binding = bindingsByKey.get(key);
        return binding != null ? binding.device() : null;
    }

    void mountDevices() {
        for (final Binding binding : bindings) {
            if (!contexts.containsKey(binding.device())) {
                final MountedContext context = new MountedContext(events, binding.key());
                contexts.put(binding.device(), context);
                binding.device().mountIO(context);
            }
        }
    }

    void unmountDevices() {
        contexts.forEach((device, context) -> device.unmountIO(context.detach()));
        contexts.clear();
    }

    void rebuild(final DeviceBusController controller) {
        final Map<IODevice, UUID> identifierByDevice = new HashMap<>();
        for (final Device device : controller.getDevices()) {
            if (!(device instanceof final IODevice ioDevice)) {
                continue;
            }

            if (ioDevice.getIOMethods().isEmpty()) {
                continue;
            }

            for (final UUID identifier : controller.getDeviceIdentifiers(device)) {
                identifierByDevice.merge(ioDevice, identifier, (a, b) -> a.compareTo(b) <= 0 ? a : b);
            }
        }

        final ArrayList<Binding> newBindings = new ArrayList<>(identifierByDevice.size());
        identifierByDevice.forEach((device, identifier) -> newBindings.add(new Binding(identifier, bindingKey(identifier, device), device)));

        newBindings.sort(Comparator.comparing(Binding::identifier).thenComparing(binding -> binding.device().getIOName()).thenComparing(binding -> binding.device().getClass().getName()));

        // All devices of a group share its identifier, so bindings are keyed per device within the
        // group. Only devices of the same name in one group remain indistinguishable.
        for (int i = newBindings.size() - 1; i > 0; i--) {
            if (newBindings.get(i).key().equals(newBindings.get(i - 1).key())) {
                final Binding dropped = newBindings.remove(i);
                LOGGER.warn("Device [{}] has the same name as another device providing a mid-level API under identifier [{}] and is not reachable from the guest.", dropped.device().getIOName(), dropped.identifier());
            }
        }

        if (newBindings.size() > MAX_DEVICES) {
            LOGGER.warn("More than {} devices provide a mid-level API; dropping {} of them.", MAX_DEVICES, newBindings.size() - MAX_DEVICES);
            newBindings.subList(MAX_DEVICES, newBindings.size()).clear();
        }

        final Map<UUID, Binding> newBindingsByKey = new HashMap<>(newBindings.size());
        final Map<UUID, Integer> newIndexByKey = new HashMap<>(newBindings.size());
        final Map<IODevice, UUID> keyByDevice = new HashMap<>(newBindings.size());
        final ArrayList<DeviceDescription> newDescriptions = new ArrayList<>(newBindings.size());
        for (int index = 0; index < newBindings.size(); index++) {
            final Binding binding = newBindings.get(index);
            newBindingsByKey.put(binding.key(), binding);
            newIndexByKey.put(binding.key(), index);
            keyByDevice.put(binding.device(), binding.key());
            newDescriptions.add(new DeviceDescription(IODeviceBusAdapter.DEVICE_CLASS, binding.device().getIOName(), binding.identifier().toString(), index));
        }

        bindings = newBindings;
        bindingsByKey = newBindingsByKey;
        indexByKey = newIndexByKey;
        descriptions = newDescriptions;

        final Iterator<Map.Entry<IODevice, MountedContext>> iterator = contexts.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<IODevice, MountedContext> entry = iterator.next();
            final IODevice device = entry.getKey();
            final MountedContext context = entry.getValue();
            final UUID key = keyByDevice.get(device);
            if (key != null) {
                context.key = key;
            } else {
                iterator.remove();
                device.unmountIO(context.detach());
            }
        }
    }

    // --------------------------------------------------------------------- //

    private static UUID bindingKey(final UUID identifier, final IODevice device) {
        return UUID.nameUUIDFromBytes((identifier + "/" + device.getIOName()).getBytes(StandardCharsets.UTF_8));
    }

    // --------------------------------------------------------------------- //

    private record Binding(UUID identifier, UUID key, IODevice device) {
    }

    private static final class MountedContext implements IOBusContext {
        @Nullable private volatile EventSink events;
        private volatile UUID key;

        MountedContext(final EventSink events, final UUID key) {
            this.events = events;
            this.key = key;
        }

        MountedContext detach() {
            events = null;
            return this;
        }

        @Override
        public boolean sendEvent(final int code, final int value) {
            if (code < 0 || code > MAX_EVENT_CODE || value < 0 || value > MAX_EVENT_VALUE) {
                throw new IllegalArgumentException("Event code [" + code + "] or value [" + value + "] out of range.");
            }

            final EventSink events = this.events;
            return events != null && events.sendEvent(key, code, value);
        }
    }
}
