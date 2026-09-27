/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.device;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.bus.IODeviceBusAdapter;
import li.cil.sedna.api.Interrupt;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.InterruptSource;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.device.bus.InterruptVectorMap;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.device.bus.DeviceEnumerator;
import li.cil.sedna.memory.SimpleMemoryMap;

import java.util.List;

public final class IODeviceBusWindow implements MemoryMappedDevice, InterruptSource {
    public static final int LENGTH = 0x1000;

    private static final int ADAPTER_OFFSET = 0x00;
    private static final int ENUMERATOR_OFFSET = 0x10;

    // --------------------------------------------------------------------- //

    private final IODeviceBusAdapter adapter;
    @Serialized
    private final DeviceEnumerator enumerator;
    private final SimpleMemoryMap registers = new SimpleMemoryMap();

    // --------------------------------------------------------------------- //

    public IODeviceBusWindow(final IODeviceBusAdapter adapter) {
        this.adapter = adapter;
        enumerator = new DeviceEnumerator(registers, List.of(adapter), interrupt -> InterruptVectorMap.NO_VECTOR);
        registers.addDevice(ADAPTER_OFFSET, adapter);
        registers.addDevice(ENUMERATOR_OFFSET, enumerator);
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public int getSupportedSizes() {
        return 1 << Sizes.SIZE_8_LOG2;
    }

    @Override
    public long load(final int offset, final int sizeLog2) throws MemoryAccessException {
        return registers.load(offset, sizeLog2);
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) throws MemoryAccessException {
        registers.store(offset, value, sizeLog2);
    }

    @Override
    public Iterable<Interrupt> getInterrupts() {
        return adapter.getInterrupts();
    }
}
