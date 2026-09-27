/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.vm.context.managed.ManagedVMContext;

import java.util.*;
import java.util.function.Function;

public final class VMDeviceRegistry {
    private final HashMap<VMDevice, ManagedVMContext> mountedDevices = new HashMap<>();
    private final LinkedHashSet<VMDevice> unmountedDevices = new LinkedHashSet<>();
    private final Function<VMDevice, OptionalLong> baseAddressProvider;

    // --------------------------------------------------------------------- //

    private final GlobalVMContext globalContext;

    // --------------------------------------------------------------------- //

    public VMDeviceRegistry(final GlobalVMContext context, final Function<VMDevice, OptionalLong> baseAddressProvider) {
        this.globalContext = context;
        this.baseAddressProvider = baseAddressProvider;
    }

    // --------------------------------------------------------------------- //

    public VMDeviceLoadResult mountDevices() {
        for (final VMDevice device : unmountedDevices) {
            final ManagedVMContext context = new ManagedVMContext(globalContext, globalContext,
                () -> baseAddressProvider.apply(device));

            final VMDeviceLoadResult result = device.mount(context);
            context.freeze();

            if (!result.wasSuccessful()) {
                // No unmount() for the device that failed: that is the counterpart of a successful mount,
                // and a failed mount is retried later. Cleaning up whatever it claimed before giving up is
                // the device's own job.
                context.invalidate();

                // Adds to the set we are iterating, which is fine only because we return right after --
                // the loop never touches its iterator again.
                unmountDevices();

                return result;
            }

            mountedDevices.put(device, context);
        }

        unmountedDevices.clear();

        globalContext.updateReservations();

        return VMDeviceLoadResult.success();
    }

    public void unmountDevices() {
        mountedDevices.forEach((device, context) -> {
            device.unmount();
            context.invalidate();
        });

        unmountedDevices.addAll(mountedDevices.keySet());
        mountedDevices.clear();
    }

    public void rebuild(final DeviceBusController controller) {
        final Set<VMDevice> devices = new LinkedHashSet<>();
        for (final Device device : controller.getDevices()) {
            if (device instanceof final VMDevice vmDevice) {
                devices.add(vmDevice);
            }
        }

        final Iterator<Map.Entry<VMDevice, ManagedVMContext>> iterator = mountedDevices.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<VMDevice, ManagedVMContext> entry = iterator.next();
            if (!devices.contains(entry.getKey())) {
                iterator.remove();
                entry.getKey().unmount();
                entry.getValue().invalidate();
            }
        }

        unmountedDevices.retainAll(devices);
        for (final VMDevice device : devices) {
            if (!mountedDevices.containsKey(device)) {
                unmountedDevices.add(device);
            }
        }
    }
}
