package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.sedna.api.Board;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;

public final class VirtualMachineDeviceBusAdapter {
    private final Board board;

    private final BitSet claimedInterrupts = new BitSet();
    private final HashMap<VMDevice, ManagedVMContext> deviceContexts = new HashMap<>();
    private final ArrayList<VMDevice> incompleteLoads = new ArrayList<>();

    ///////////////////////////////////////////////////////////////////

    // This is a superset of allocatedInterrupts. We use this so that after loading we
    // avoid potentially new devices (due external code changes, etc.) to grab interrupts
    // previously used by other devices. Only claiming interrupts explicitly will allow
    // grabbing reserved interrupts.
    @Serialized @SuppressWarnings("FieldMayBeFinal") private BitSet reservedInterrupts = new BitSet();

    ///////////////////////////////////////////////////////////////////

    public VirtualMachineDeviceBusAdapter(final Board board) {
        this.board = board;
        this.claimedInterrupts.set(0);
    }

    public int claimInterrupt() {
        return claimInterrupt(claimedInterrupts.nextClearBit(0));
    }

    public int claimInterrupt(final int interrupt) {
        if (interrupt < 1 || interrupt >= R5PlatformLevelInterruptController.INTERRUPT_COUNT) {
            throw new IllegalArgumentException();
        }

        claimedInterrupts.set(interrupt);
        return interrupt;
    }

    public boolean load() {
        for (int i = incompleteLoads.size() - 1; i >= 0; i--) {
            final VMDevice device = incompleteLoads.remove(i);

            final ManagedVMContext context = new ManagedVMContext(
                    board, claimedInterrupts, reservedInterrupts);
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
        reservedInterrupts.or(claimedInterrupts);

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
