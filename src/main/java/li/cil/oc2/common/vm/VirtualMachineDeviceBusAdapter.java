package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.device.sedna.SednaDevice;
import li.cil.oc2.api.bus.device.sedna.SednaDeviceLoadResult;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;

import java.util.*;

public final class VirtualMachineDeviceBusAdapter {
    private final MemoryMap memoryMap;
    private final InterruptController interruptController;

    private final BitSet allocatedInterrupts = new BitSet();
    private final HashMap<SednaDevice, ManagedVirtualMachineContext> deviceContexts = new HashMap<>();
    private final ArrayList<SednaDevice> incompleteLoads = new ArrayList<>();

    // This is a superset of allocatedInterrupts. We use this so that after loading we
    // avoid potentially new devices (due external code changes, etc.) to grab interrupts
    // previously used by other devices. Only claiming interrupts explicitly will allow
    // grabbing reserved interrupts.
    @Serialized @SuppressWarnings("FieldMayBeFinal") private BitSet reservedInterrupts = new BitSet();

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
            final SednaDevice device = incompleteLoads.remove(i);

            final ManagedVirtualMachineContext context = new ManagedVirtualMachineContext(
                    memoryMap, interruptController, allocatedInterrupts, reservedInterrupts);
            deviceContexts.put(device, context);

            final SednaDeviceLoadResult result = device.load(context);
            context.freeze();

            if (!result.wasSuccessful()) {
                context.invalidate();
                incompleteLoads.add(device);
            }
        }

        return incompleteLoads.isEmpty();
    }

    public void unload() {
        deviceContexts.forEach((device, context) -> {
            context.invalidate();
            device.unload();
        });

        incompleteLoads.clear();
        incompleteLoads.addAll(deviceContexts.keySet());
    }

    public void setDevices(final Collection<Device> devices) {
        final HashSet<SednaDevice> oldDevices = new HashSet<>(deviceContexts.keySet());
        final HashSet<SednaDevice> newDevices = new HashSet<>();
        for (final Device device : devices) {
            if (device instanceof SednaDevice) {
                newDevices.add((SednaDevice) device);
            }
        }

        final HashSet<SednaDevice> removedDevices = new HashSet<>(oldDevices);
        removedDevices.removeAll(newDevices);
        for (final SednaDevice device : removedDevices) {
            deviceContexts.remove(device).invalidate();
            incompleteLoads.remove(device);
            device.unload();
        }

        final HashSet<SednaDevice> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(oldDevices);
        for (final SednaDevice device : addedDevices) {
            deviceContexts.put(device, null);
            incompleteLoads.add(device);
        }

        reservedInterrupts.clear();
        reservedInterrupts.or(allocatedInterrupts);
    }
}
