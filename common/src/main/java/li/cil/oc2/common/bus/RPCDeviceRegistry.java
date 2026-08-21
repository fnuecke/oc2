/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceList;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceWithIdentifier;

import javax.annotation.Nullable;
import java.util.*;

final class RPCDeviceRegistry {
    private final ArrayList<RPCDeviceWithIdentifier> devices = new ArrayList<>();
    private final HashMap<UUID, RPCDeviceList> devicesById = new HashMap<>();
    private final Set<RPCDeviceList> unmountedDevices = new HashSet<>();
    private final Set<RPCDeviceList> mountedDevices = new HashSet<>();

    @Serialized
    private int generation; // bumped whenever the device list changes, sent with every reply

    // --------------------------------------------------------------------- //

    int generation() {
        return generation;
    }

    List<RPCDeviceWithIdentifier> devices() {
        return devices;
    }

    @Nullable
    RPCDeviceList byId(final UUID identifier) {
        return devicesById.get(identifier);
    }

    void mountAll() {
        for (final RPCDevice device : unmountedDevices) {
            device.mount();
        }

        mountedDevices.addAll(unmountedDevices);
        unmountedDevices.clear();
    }

    void unmountAll() {
        for (final RPCDevice device : mountedDevices) {
            device.unmount();
        }

        unmountedDevices.addAll(mountedDevices);
        mountedDevices.clear();
    }

    void disposeAll() {
        unmountAll();

        unmountedDevices.forEach(RPCDeviceList::dispose);
    }

    void rebuild(final DeviceBusController controller) {
        // How device grouping works:
        // Each device can have multiple UUIDs due to being attached to multiple bus elements.
        // There is no guarantee that for each device D1 present on bus elements E1 and E2,
        // where device D2 is present on E1 it will also be present on E2. This is completely
        // up to the device providers.
        // Therefore, we must group all devices by their identifiers to then remove duplicate
        // groups. This is fragile because it will depend on the order the devices appear in
        // the list. However, since we add devices to bus elements in the order of their
        // providers, then add devices to the controller in the order of their elements, this
        // will work. And even if it does not, it only leads to duplicate devices popping up
        // in the VM, which, while annoying, is not breaking anything.
        // In a final step, when we know which devices are duplicates and what identifiers
        // they have, we pick a single identifier in a deterministic way, given the list of
        // identifiers is the same.

        final HashMap<UUID, ArrayList<RPCDevice>> devicesByIdentifier = new HashMap<>();
        for (final Device device : controller.getDevices()) {
            if (device instanceof final RPCDevice rpcDevice) {
                final Set<UUID> identifiers = controller.getDeviceIdentifiers(device);
                for (final UUID identifier : identifiers) {
                    devicesByIdentifier
                        .computeIfAbsent(identifier, unused -> new ArrayList<>())
                        .add(rpcDevice);
                }
            }
        }

        final HashMap<RPCDeviceList, ArrayList<UUID>> identifiersByDevice = new HashMap<>();
        devicesByIdentifier.forEach((identifier, devices) -> {
            final RPCDeviceList device = new RPCDeviceList(devices);

            // If there are no methods we have either no devices at all, or all synthetic
            // devices, i.e. devices that only contribute type names, but have no methods
            // to call. We do not expose these to avoid cluttering the device list.
            if (device.getMethodGroups().isEmpty()) {
                return;
            }

            identifiersByDevice
                .computeIfAbsent(device, unused -> new ArrayList<>())
                .add(identifier);
        });

        // Rebuild devices lists.
        devices.clear();
        devicesById.clear();

        final Set<RPCDeviceList> devices = new HashSet<>();
        identifiersByDevice.forEach((device, identifiers) -> {
            final UUID identifier = selectIdentifierDeterministically(identifiers);
            this.devices.add(new RPCDeviceWithIdentifier(identifier, device));
            devicesById.put(identifier, device);
            devices.add(device);

            // Add to set of unmounted devices if we don't already track it. It's a set, so
            // there won't be duplicates in the unmounted set due to this.
            if (!mountedDevices.contains(device)) {
                unmountedDevices.add(device);
            }
        });

        // Remove devices from mounted set, call appropriate callbacks.
        final HashSet<RPCDeviceList> removedMountedDevices = new HashSet<>(mountedDevices);
        removedMountedDevices.removeAll(devices);
        mountedDevices.removeAll(removedMountedDevices);
        removedMountedDevices.forEach(RPCDeviceList::unmount);

        // Remove devices from unmounted set.
        unmountedDevices.retainAll(devices);

        generation++;
    }

    // --------------------------------------------------------------------- //

    private static UUID selectIdentifierDeterministically(final ArrayList<UUID> identifiers) {
        UUID lowestIdentifier = identifiers.get(0);
        for (int i = 1; i < identifiers.size(); i++) {
            final UUID identifier = identifiers.get(i);
            if (identifier.compareTo(lowestIdentifier) < 0) {
                lowestIdentifier = identifier;
            }
        }
        return lowestIdentifier;
    }
}
