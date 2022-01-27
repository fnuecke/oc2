package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.vm.context.managed.ManagedVMContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.OptionalLong;

public final class VMDeviceBusAdapter {
    private final HashMap<VMDevice, ManagedVMContext> mountedDevices = new HashMap<>();
    private final ArrayList<VMDevice> unmountedDevices = new ArrayList<>();
    private BaseAddressProvider baseAddressProvider = unused -> OptionalLong.empty();

    ///////////////////////////////////////////////////////////////////

    private final GlobalVMContext globalContext;

    ///////////////////////////////////////////////////////////////////

    public VMDeviceBusAdapter(final GlobalVMContext context) {
        this.globalContext = context;
    }

    ///////////////////////////////////////////////////////////////////

    public void setBaseAddressProvider(final BaseAddressProvider provider) {
        baseAddressProvider = provider;
    }

    public VMDeviceLoadResult mountDevices() {
        globalContext.joinWorkerThread();

        for (final VMDevice device : unmountedDevices) {
            final ManagedVMContext context = new ManagedVMContext(globalContext, globalContext,
                () -> baseAddressProvider.getBaseAddress(device));

            final VMDeviceLoadResult result = device.mount(context);
            context.freeze();

            if (!result.wasSuccessful()) {
                context.invalidate();
                mountedDevices.forEach((mountedDevice, mountedContext) -> {
                    mountedDevice.unmount();
                    mountedDevice.dispose();
                    mountedContext.invalidate();
                });
                mountedDevices.clear();
                return result;
            }

            mountedDevices.put(device, context);
        }

        unmountedDevices.clear();

        globalContext.updateReservations();

        return VMDeviceLoadResult.success();
    }

    public void unmountDevices() {
        globalContext.joinWorkerThread();

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
        globalContext.joinWorkerThread();

        for (final Device device : devices) {
            if (device instanceof final VMDevice vmDevice) {
                // Add to set of unmounted devices if we don't already track it. It's a set, so
                // there won't be duplicates in the unmounted set due to this.
                if (!mountedDevices.containsKey(vmDevice)) {
                    unmountedDevices.add(vmDevice);
                }
            }
        }
    }

    public void removeDevices(final Collection<Device> devices) {
        globalContext.joinWorkerThread();

        for (final Device device : devices) {
            if (device instanceof final VMDevice vmDevice) {
                final ManagedVMContext context = mountedDevices.remove(vmDevice);
                if (context != null) {
                    vmDevice.unmount();
                    vmDevice.dispose();
                    context.invalidate();
                } else {
                    unmountedDevices.remove(vmDevice);
                    vmDevice.dispose();
                }
            }
        }
    }
}
