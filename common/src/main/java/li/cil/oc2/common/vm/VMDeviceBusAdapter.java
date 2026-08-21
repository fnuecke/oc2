/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.vm.context.managed.ManagedVMContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.OptionalLong;

public final class VMDeviceBusAdapter {
    private final HashMap<VMDevice, ManagedVMContext> mountedDevices = new HashMap<>();
    private final LinkedHashSet<VMDevice> unmountedDevices = new LinkedHashSet<>();
    private BaseAddressProvider baseAddressProvider = unused -> OptionalLong.empty();

    // --------------------------------------------------------------------- //

    private final GlobalVMContext globalContext;

    // --------------------------------------------------------------------- //

    public VMDeviceBusAdapter(final GlobalVMContext context) {
        this.globalContext = context;
    }

    // --------------------------------------------------------------------- //

    public void setBaseAddressProvider(final BaseAddressProvider provider) {
        baseAddressProvider = provider;
    }

    public VMDeviceLoadResult mountDevices() {
        for (final VMDevice device : unmountedDevices) {
            final ManagedVMContext context = new ManagedVMContext(globalContext, globalContext,
                () -> baseAddressProvider.getBaseAddress(device));

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

    public void disposeDevices() {
        unmountDevices();

        unmountedDevices.forEach(VMDevice::dispose);
    }

    public void addDevices(final Collection<Device> devices) {
        for (final Device device : devices) {
            if (device instanceof final VMDevice vmDevice) {
                // Add to the set of unmounted devices if we don't already track it.
                if (!mountedDevices.containsKey(vmDevice)) {
                    unmountedDevices.add(vmDevice);
                }
            }
        }
    }

    public void removeDevices(final Collection<Device> devices) {
        for (final Device device : devices) {
            if (device instanceof final VMDevice vmDevice) {
                final ManagedVMContext context = mountedDevices.remove(vmDevice);
                if (context != null) {
                    vmDevice.unmount();
                    context.invalidate();
                } else {
                    unmountedDevices.remove(vmDevice);
                }
            }
        }
    }
}
