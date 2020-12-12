package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;

public final class VirtualMachineDeviceBusAdapter {
    private final MemoryMap memoryMap;
    private final InterruptController interruptController;

    private final BitSet allocatedInterrupts = new BitSet();
    private final HashMap<VMDevice, ManagedVMContext> deviceContexts = new HashMap<>();
    private final ArrayList<VMDevice> incompleteLoads = new ArrayList<>();

    ///////////////////////////////////////////////////////////////////

    // This is a superset of allocatedInterrupts. We use this so that after loading we
    // avoid potentially new devices (due external code changes, etc.) to grab interrupts
    // previously used by other devices. Only claiming interrupts explicitly will allow
    // grabbing reserved interrupts.
    @Serialized @SuppressWarnings("FieldMayBeFinal") private BitSet reservedInterrupts = new BitSet();

    ///////////////////////////////////////////////////////////////////

    public VirtualMachineDeviceBusAdapter(final MemoryMap memoryMap, final InterruptController interruptController) {
        this.memoryMap = memoryMap;
        this.interruptController = interruptController;
    }

    public int claimInterrupt() {
        return claimInterrupt(allocatedInterrupts.nextClearBit(0) + 1);
    }

    public int claimInterrupt(final int interrupt) {
        if (interrupt < 1 || interrupt > R5PlatformLevelInterruptController.INTERRUPT_COUNT) {
            throw new IllegalArgumentException();
        }

        allocatedInterrupts.set(interrupt - 1);
        return interrupt;
    }

    public boolean load() {
        for (int i = incompleteLoads.size() - 1; i >= 0; i--) {
            final VMDevice device = incompleteLoads.remove(i);

            final ManagedVMContext context = new ManagedVMContext(
                    memoryMap, interruptController, allocatedInterrupts, reservedInterrupts);
            deviceContexts.put(device, context);

            final VMDeviceLoadResult result = device.load(context);
            context.freeze();

            if (!result.wasSuccessful()) {
                context.invalidate();
                incompleteLoads.add(device);
            }
        }

        if (!incompleteLoads.isEmpty()) {
            return false;
        }

        reservedInterrupts.clear();
        reservedInterrupts.or(allocatedInterrupts);

        return true;
    }

    public void unload() {
        deviceContexts.forEach((device, context) -> {
            context.invalidate();
            device.unload();
        });

        incompleteLoads.clear();
        incompleteLoads.addAll(deviceContexts.keySet());
    }

    public void addDevices(final Set<Device> devices) {
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

    public void removeDevices(final Set<Device> devices) {
        for (final Device device : devices) {
            if (device instanceof VMDevice) {
                final VMDevice vmDevice = (VMDevice) device;

                final ManagedVMContext context = deviceContexts.remove(vmDevice);
                if (context != null) {
                    context.invalidate();
                }

                incompleteLoads.remove(vmDevice);

                vmDevice.unload();
            }
        }
    }
}
