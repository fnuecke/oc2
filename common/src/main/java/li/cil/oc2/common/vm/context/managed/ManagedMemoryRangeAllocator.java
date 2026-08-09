/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context.managed;

import li.cil.oc2.api.bus.device.vm.context.MemoryRangeAllocator;
import li.cil.oc2.common.vm.context.MemoryRangeManager;
import li.cil.sedna.api.device.MemoryMappedDevice;

import java.util.ArrayList;
import java.util.OptionalLong;
import java.util.function.Supplier;

final class ManagedMemoryRangeAllocator implements MemoryRangeAllocator {
    private final MemoryRangeAllocator parent;
    private final MemoryRangeManager memoryRangeManager;
    private final Supplier<OptionalLong> baseAddressSupplier;
    private final ArrayList<MemoryMappedDevice> managedDevices = new ArrayList<>();
    private boolean isFrozen;

    ///////////////////////////////////////////////////////////////////

    public ManagedMemoryRangeAllocator(final MemoryRangeAllocator parent,
                                       final MemoryRangeManager memoryRangeManager,
                                       final Supplier<OptionalLong> baseAddressSupplier) {
        this.parent = parent;
        this.memoryRangeManager = memoryRangeManager;
        this.baseAddressSupplier = baseAddressSupplier;
    }

    ///////////////////////////////////////////////////////////////////

    public void freeze() {
        isFrozen = true;
    }

    public void invalidate() {
        for (final MemoryMappedDevice device : managedDevices) {
            memoryRangeManager.releaseMemoryRange(device);
        }
        managedDevices.clear();
    }

    @Override
    public boolean claimMemoryRange(final long address, final MemoryMappedDevice device) {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        if (parent.claimMemoryRange(address, device)) {
            managedDevices.add(device);
            return true;
        }

        return false;
    }

    @Override
    public OptionalLong claimMemoryRange(final MemoryMappedDevice device) {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        final OptionalLong baseAddress = baseAddressSupplier.get();
        if (baseAddress.isPresent()) {
            final OptionalLong address = memoryRangeManager.findMemoryRange(device, baseAddress.getAsLong());
            if (address.isPresent() && parent.claimMemoryRange(address.getAsLong(), device)) {
                managedDevices.add(device);
                return address;
            }
        } else {
            final OptionalLong address = parent.claimMemoryRange(device);
            if (address.isPresent()) {
                managedDevices.add(device);
                return address;
            }
        }

        return OptionalLong.empty();
    }
}
