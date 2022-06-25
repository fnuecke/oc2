/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context.global;

import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import li.cil.oc2.api.bus.device.vm.context.MemoryRangeAllocator;
import li.cil.oc2.common.vm.context.MemoryRangeManager;
import li.cil.sedna.api.Board;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryRange;
import li.cil.sedna.api.memory.MemoryRangeAllocationStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;

final class GlobalMemoryRangeAllocator implements MemoryRangeAllocator, MemoryRangeManager {
    private final Board board;
    private final ArrayList<MemoryRange> reservedMemoryRanges;
    private final Object2LongArrayMap<MemoryMappedDevice> claimedMemoryRanges = new Object2LongArrayMap<>();

    ///////////////////////////////////////////////////////////////////

    public GlobalMemoryRangeAllocator(final Board board, final ArrayList<MemoryRange> reservedMemoryRanges) {
        this.board = board;
        this.reservedMemoryRanges = reservedMemoryRanges;
    }

    ///////////////////////////////////////////////////////////////////

    public Collection<MemoryRange> getClaimedMemoryRanges() {
        final ArrayList<MemoryRange> result = new ArrayList<>();
        for (final Object2LongMap.Entry<MemoryMappedDevice> entry : claimedMemoryRanges.object2LongEntrySet()) {
            final MemoryMappedDevice device = entry.getKey();
            final long address = entry.getLongValue();
            result.add(MemoryRange.at(address, device.getLength()));
        }
        return result;
    }

    public void invalidate() {
        for (final MemoryMappedDevice device : claimedMemoryRanges.keySet()) {
            board.removeDevice(device);
        }
        claimedMemoryRanges.clear();
    }

    @Override
    public boolean claimMemoryRange(final long address, final MemoryMappedDevice device) {
        if (board.addDevice(address, device)) {
            claimedMemoryRanges.put(device, address);
            return true;
        }

        return false;
    }

    @Override
    public OptionalLong claimMemoryRange(final MemoryMappedDevice device) {
        final OptionalLong address = board.addDevice(device);
        if (address.isPresent()) {
            claimedMemoryRanges.put(device, address.getAsLong());
            return address;
        }

        return OptionalLong.empty();
    }

    @Override
    public OptionalLong findMemoryRange(final MemoryMappedDevice device, final long start) {
        return board.getAllocationStrategy().findMemoryRange(device, range -> {
            for (final MemoryRange reservedRange : reservedMemoryRanges) {
                if (reservedRange.intersects(range)) {
                    return Optional.of(reservedRange);
                }
            }
            return MemoryRangeAllocationStrategy.getMemoryMapIntersectionProvider(board.getMemoryMap()).apply(range);
        }, start);
    }

    @Override
    public void releaseMemoryRange(final MemoryMappedDevice device) {
        board.removeDevice(device);
        claimedMemoryRanges.removeLong(device);
    }
}
