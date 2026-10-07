/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IOBusContext;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.common.bus.device.DeviceWithAliases;
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
        final HashMap<IODevice, HashSet<UUID>> identifiersByDevice = new HashMap<>();
        final HashMap<UUID, TreeSet<String>> namesByIdentifier = new HashMap<>();
        for (final Device device : controller.getDevices()) {
            final Set<UUID> identifiers = controller.getDeviceIdentifiers(device);
            if (device instanceof final IODevice ioDevice && !ioDevice.getIOMethods().isEmpty()) {
                if (!DeviceDescription.isValidName(ioDevice.getIOName())) {
                    LOGGER.warn("Device [{}] has a name [{}] that is empty or not printable ASCII and is not reachable from the guest.", device.getClass().getName(), ioDevice.getIOName());
                } else {
                    for (final UUID identifier : identifiers) {
                        identifiersByDevice
                            .computeIfAbsent(ioDevice, unused -> new HashSet<>())
                            .add(identifier);
                    }
                }
            }
            if (device instanceof final DeviceWithAliases deviceWithAliases) {
                for (final String alias : deviceWithAliases.getAliases()) {
                    if (!DeviceDescription.isValidName(alias)) {
                        LOGGER.warn("Device [{}] has an alias [{}] that is empty or not printable ASCII and is not listed for the guest.", device.getClass().getName(), alias);
                        continue;
                    }
                    for (final UUID identifier : identifiers) {
                        namesByIdentifier
                            .computeIfAbsent(identifier, unused -> new TreeSet<>())
                            .add(alias);
                    }
                }
            }
        }

        final ArrayList<Binding> newBindings = new ArrayList<>(identifiersByDevice.size());
        identifiersByDevice.forEach((device, identifiers) -> {
            final UUID identifier = Collections.min(identifiers);
            newBindings.add(new Binding(identifier, bindingKey(identifier, device), device, names(device, identifiers, namesByIdentifier)));
        });

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
            newDescriptions.add(new DeviceDescription(IODeviceBusAdapter.DEVICE_CLASS, binding.names(), binding.identifier().toString(), index));
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

    private static List<String> names(final IODevice device, final Set<UUID> identifiers, final Map<UUID, TreeSet<String>> namesByIdentifier) {
        final TreeSet<String> aliases = new TreeSet<>();
        for (final UUID identifier : identifiers) {
            final TreeSet<String> identifierNames = namesByIdentifier.get(identifier);
            if (identifierNames != null) {
                aliases.addAll(identifierNames);
            }
        }

        final ArrayList<String> names = new ArrayList<>(aliases.size() + 1);
        names.add(device.getIOName());
        names.addAll(aliases);
        return names;
    }

    private static UUID bindingKey(final UUID identifier, final IODevice device) {
        return UUID.nameUUIDFromBytes((identifier + "/" + device.getIOName()).getBytes(StandardCharsets.UTF_8));
    }

    // --------------------------------------------------------------------- //

    private record Binding(UUID identifier, UUID key, IODevice device, List<String> names) {
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
