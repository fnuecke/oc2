/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context.managed;

import li.cil.oc2.api.bus.device.vm.context.MemoryAllocator;
import li.cil.oc2.common.vm.Allocator;

import java.util.ArrayList;
import java.util.UUID;

final class ManagedMemoryAllocator implements MemoryAllocator {
    private final ArrayList<UUID> claimedMemory = new ArrayList<>();
    private boolean isFrozen;

    ///////////////////////////////////////////////////////////////////

    public void freeze() {
        isFrozen = true;
    }

    public void invalidate() {
        for (final UUID handle : claimedMemory) {
            Allocator.freeMemory(handle);
        }

        claimedMemory.clear();
    }

    @Override
    public boolean claimMemory(final int size) {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        final UUID handle = Allocator.createHandle();
        if (!Allocator.claimMemory(handle, size)) {
            return false;
        }

        claimedMemory.add(handle);
        return true;
    }
}
