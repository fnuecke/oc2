package li.cil.oc2.common.vm;

import com.google.common.eventbus.EventBus;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.event.VMLifecycleEvent;
import li.cil.sedna.api.Board;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public final class VirtualMachineDeviceBusAdapter {
    private final Board board;

    private final EventBus eventBus = new EventBus();
    private final ManagedVMContext globalContext;
    private final BitSet claimedInterrupts = new BitSet();
    private final HashMap<VMDevice, ManagedVMContext> deviceContexts = new HashMap<>();
    private final ArrayList<VMDevice> incompleteLoads = new ArrayList<>();

    private DefaultAddressProvider defaultAddressProvider = unused -> OptionalLong.empty();

    ///////////////////////////////////////////////////////////////////

    // This is a superset of allocatedInterrupts. We use this so that after loading we
    // avoid potentially new devices (due external code changes, etc.) to grab interrupts
    // previously used by other devices. Only claiming interrupts explicitly will allow
    // grabbing reserved interrupts.
    @Serialized @SuppressWarnings("FieldMayBeFinal") private BitSet reservedInterrupts = new BitSet();

    ///////////////////////////////////////////////////////////////////

    public VirtualMachineDeviceBusAdapter(final Board board) {
        this.board = board;
        this.globalContext = new ManagedVMContext(board, claimedInterrupts, reservedInterrupts, eventBus);
        this.claimedInterrupts.set(0);
    }

    ///////////////////////////////////////////////////////////////////

    public VMContext getGlobalContext() {
        return globalContext;
    }

    public void setDefaultAddressProvider(final DefaultAddressProvider provider) {
        defaultAddressProvider = provider;
    }

    public VMDeviceLoadResult load() {
        for (int i = 0; i < incompleteLoads.size(); i++) {
            final VMDevice device = incompleteLoads.get(i);

            final ManagedVMContext context = new ManagedVMContext(
                    board, claimedInterrupts, reservedInterrupts, eventBus,
                    (memoryMappedDevice) -> defaultAddressProvider.getDefaultAddress(device));

            deviceContexts.put(device, context);

            final VMDeviceLoadResult result = device.load(context);
            context.freeze();

            if (!result.wasSuccessful()) {
                for (; i >= 0; i--) {
                    deviceContexts.get(incompleteLoads.get(i)).invalidate();
                }
                return result;
            }
        }

        incompleteLoads.clear();

        reservedInterrupts.clear();
        reservedInterrupts.or(claimedInterrupts);

        return VMDeviceLoadResult.success();
    }

    public void unload() {
        for (final VMDevice device : deviceContexts.keySet()) {
            device.unload();
        }

        suspend();
    }

    public void suspend() {
        deviceContexts.forEach((device, context) -> {
            if (context != null) {
                context.invalidate();
            }
        });

        incompleteLoads.clear();
        incompleteLoads.addAll(deviceContexts.keySet());
    }

    public void postLifecycleEvent(final VMLifecycleEvent event) {
        eventBus.post(event);
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

                vmDevice.unload();

                final ManagedVMContext context = deviceContexts.remove(vmDevice);
                if (context != null) {
                    context.invalidate();
                }

                incompleteLoads.remove(vmDevice);
            }
        }
    }
}
