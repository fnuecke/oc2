package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.sedna.api.device.MemoryMappedDevice;

import java.util.OptionalLong;

public final class OptionalAddress {
    private Long value;

    public boolean isPresent() {
        return value != null;
    }

    public long getAsLong() {
        return value;
    }

    public void set(final long value) {
        this.value = value;
    }

    public void clear() {
        this.value = null;
    }

    public boolean claim(final VMContext context, final MemoryMappedDevice device) {
        final OptionalLong claimedAddress;
        if (value == null) {
            claimedAddress = context.getMemoryRangeAllocator().claimMemoryRange(device);
        } else {
            claimedAddress = context.getMemoryRangeAllocator().claimMemoryRange(value, device);
        }

        if (claimedAddress.isPresent()) {
            value = claimedAddress.getAsLong();
            return true;
        } else {
            return false;
        }
    }
}
