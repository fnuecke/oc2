/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context.global;

import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import li.cil.oc2.api.bus.device.vm.context.MemoryRangeAllocator;
import li.cil.oc2.common.vm.context.MemoryRangeManager;
import li.cil.sedna.api.DeviceBus;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MappedMemoryRange;
import li.cil.sedna.api.memory.MemoryRange;
import li.cil.sedna.api.memory.MemoryRangeAllocationStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;

final class GlobalMemoryRangeAllocator implements MemoryRangeAllocator, MemoryRangeManager {
    private final DeviceBus bus;
    private final ArrayList<MemoryRange> reservedMemoryRanges;
    private final Object2LongArrayMap<MemoryMappedDevice> claimedMemoryRanges = new Object2LongArrayMap<>();

    // --------------------------------------------------------------------- //

    public GlobalMemoryRangeAllocator(final DeviceBus bus, final ArrayList<MemoryRange> reservedMemoryRanges) {
        this.bus = bus;
        this.reservedMemoryRanges = reservedMemoryRanges;
    }

    // --------------------------------------------------------------------- //

    public Collection<MemoryRange> getClaimedMemoryRanges() {
        final ArrayList<MemoryRange> result = new ArrayList<>();
        for (final Object2LongMap.Entry<MemoryMappedDevice> entry : claimedMemoryRanges.object2LongEntrySet()) {
            final MemoryMappedDevice device = entry.getKey();
            final long address = entry.getLongValue();
            final MappedMemoryRange mapped = bus.getMemoryMap().getMemoryRange(device).orElse(null);
            result.add(MemoryRange.at(address, mapped != null ? mapped.size() : device.getLength()));
        }
        return result;
    }

    public void invalidate() {
        for (final MemoryMappedDevice device : claimedMemoryRanges.keySet()) {
            bus.removeDevice(device);
        }
        claimedMemoryRanges.clear();
    }

    @Override
    public boolean claimMemoryRange(final long address, final MemoryMappedDevice device) {
        if (bus.addDevice(address, device)) {
            claimedMemoryRanges.put(device, address);
            return true;
        }

        return false;
    }

    @Override
    public OptionalLong claimMemoryRange(final MemoryMappedDevice device) {
        final OptionalLong address = bus.addDevice(device);
        if (address.isPresent()) {
            claimedMemoryRanges.put(device, address.getAsLong());
            return address;
        }

        return OptionalLong.empty();
    }

    @Override
    public OptionalLong findMemoryRange(final MemoryMappedDevice device, final long start) {
        return bus.getAllocationStrategy().findMemoryRange(device, range -> {
            for (final MemoryRange reservedRange : reservedMemoryRanges) {
                if (reservedRange.intersects(range)) {
                    return Optional.of(reservedRange);
                }
            }
            return MemoryRangeAllocationStrategy.getMemoryMapIntersectionProvider(bus.getMemoryMap()).apply(range);
        }, start);
    }

    @Override
    public void releaseMemoryRange(final MemoryMappedDevice device) {
        bus.removeDevice(device);
        claimedMemoryRanges.removeLong(device);
    }
}
