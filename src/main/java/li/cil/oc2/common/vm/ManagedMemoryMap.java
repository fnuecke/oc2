package li.cil.oc2.common.vm;

import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.api.memory.MemoryRange;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalLong;

final class ManagedMemoryMap implements MemoryMap {
    private final MemoryMap memoryMap;
    private final ArrayList<MemoryMappedDevice> managedDevices = new ArrayList<>();
    private boolean isFrozen;

    ///////////////////////////////////////////////////////////////////

    ManagedMemoryMap(final MemoryMap memoryMap) {
        this.memoryMap = memoryMap;
    }

    public void freeze() {
        isFrozen = true;
    }

    public void invalidate() {
        for (final MemoryMappedDevice device : managedDevices) {
            memoryMap.removeDevice(device);
        }
    }

    @Override
    public OptionalLong findFreeRange(final long start, final long end, final int size) {
        return memoryMap.findFreeRange(start, end, size);
    }

    @Override
    public boolean addDevice(final long address, final MemoryMappedDevice device) {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        if (memoryMap.addDevice(address, device)) {
            managedDevices.add(device);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removeDevice(final MemoryMappedDevice device) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MemoryRange> getMemoryRange(final MemoryMappedDevice device) {
        return memoryMap.getMemoryRange(device);
    }

    @Nullable
    @Override
    public MemoryRange getMemoryRange(final long address) {
        return memoryMap.getMemoryRange(address);
    }

    @Override
    public void setDirty(final MemoryRange range, final int offset) {
        memoryMap.setDirty(range, offset);
    }

    @Override
    public long load(final long address, final int sizeLog2) throws MemoryAccessException {
        return memoryMap.load(address, sizeLog2);
    }

    @Override
    public void store(final long address, final long value, final int sizeLog2) throws MemoryAccessException {
        memoryMap.store(address, value, sizeLog2);
    }
}
