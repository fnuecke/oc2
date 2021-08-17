package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
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
    private final HashMap<VMDevice, ManagedVMContext> deviceContexts = new HashMap<>();
    private final ArrayList<VMDevice> incompleteLoads = new ArrayList<>();
    private BaseAddressProvider baseAddressProvider = unused -> OptionalLong.empty();

    ///////////////////////////////////////////////////////////////////

    @Serialized @SuppressWarnings("FieldMayBeFinal")
    private GlobalVMContext globalContext;

    ///////////////////////////////////////////////////////////////////

    public VMDeviceBusAdapter(final GlobalVMContext context) {
        this.globalContext = context;
    }

    ///////////////////////////////////////////////////////////////////

    public void setBaseAddressProvider(final BaseAddressProvider provider) {
        baseAddressProvider = provider;
    }

    public VMDeviceLoadResult mount() {
        for (int i = 0; i < incompleteLoads.size(); i++) {
            final VMDevice device = incompleteLoads.get(i);

            final ManagedVMContext context = new ManagedVMContext(globalContext, globalContext,
                    () -> baseAddressProvider.getBaseAddress(device));

            deviceContexts.put(device, context);

            final VMDeviceLoadResult result = device.mount(context);
            context.freeze();

            if (!result.wasSuccessful()) {
                for (; i >= 0; i--) {
                    deviceContexts.get(incompleteLoads.get(i)).invalidate();
                }
                return result;
            }
        }

        incompleteLoads.clear();

        globalContext.updateReservations();

        return VMDeviceLoadResult.success();
    }

    public void unmount() {
        for (final VMDevice device : deviceContexts.keySet()) {
            device.unmount();
        }

        unload();
    }

    public void suspend() {
        for (final VMDevice device : deviceContexts.keySet()) {
            device.suspend();
        }

        unload();
    }

    public void addDevices(final Collection<Device> devices) {
        for (final Device device : devices) {
            if (device instanceof VMDevice) {
                final VMDevice vmDevice = (VMDevice) device;

                final ManagedVMContext context = deviceContexts.put(vmDevice, null);
                if (context != null) {
                    context.invalidate();
                }

                incompleteLoads.add(vmDevice);
            }
        }
    }

    public void removeDevices(final Collection<Device> devices) {
        for (final Device device : devices) {
            if (device instanceof VMDevice) {
                final VMDevice vmDevice = (VMDevice) device;

                vmDevice.unmount();

                final ManagedVMContext context = deviceContexts.remove(vmDevice);
                if (context != null) {
                    context.invalidate();
                }

                incompleteLoads.remove(vmDevice);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void unload() {
        deviceContexts.forEach((device, context) -> {
            if (context != null) {
                context.invalidate();
            }
        });

        incompleteLoads.clear();
        incompleteLoads.addAll(deviceContexts.keySet());
    }
}
