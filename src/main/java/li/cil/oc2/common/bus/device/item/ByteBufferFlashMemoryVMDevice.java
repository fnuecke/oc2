package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.device.flash.FlashMemoryDevice;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.OptionalLong;

public final class ByteBufferFlashMemoryVMDevice extends IdentityProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener, ItemDevice {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////

    public static final String DATA_TAG_NAME = "data";

    ///////////////////////////////////////////////////////////////

    private final int size;
    private MemoryMap memoryMap;
    private ByteBuffer data;
    private FlashMemoryDevice device;

    ///////////////////////////////////////////////////////////////

    // Online persisted data.
    private CompoundNBT deviceNbt;
    private Long address;

    ///////////////////////////////////////////////////////////////

    public ByteBufferFlashMemoryVMDevice(final ItemStack identity, final int size) {
        super(identity);
        this.size = size;
    }

    ///////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult load(final VMContext context) {
        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        if (!claimAddress(context)) {
            return VMDeviceLoadResult.fail();
        }

        loadPersistedState();

        memoryMap = context.getMemoryMap();

        return VMDeviceLoadResult.success();
    }

    @Override
    public void handleLifecycleEvent(final VMDeviceLifecycleEventType event) {
        switch (event) {
            case INITIALIZING:
                // TODO Have start address passed with event?
                copyDataToMemory(0x80000000L);
                break;
            case UNLOAD:
                unload();
                break;
        }
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT nbt = new CompoundNBT();

        if (device != null) {
            nbt.putByteArray(DATA_TAG_NAME, device.getData().array());
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(final CompoundNBT nbt) {

    }

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(size)) {
            return false;
        }

        data = ByteBuffer.allocate(size);
        device = new FlashMemoryDevice(data);

        return true;
    }

    private boolean claimAddress(final VMContext context) {
        final OptionalLong claimedAddress;
        if (this.address != null) {
            claimedAddress = context.getMemoryRangeAllocator().claimMemoryRange(this.address, device);
        } else {
            claimedAddress = context.getMemoryRangeAllocator().claimMemoryRange(device);
        }

        if (!claimedAddress.isPresent()) {
            return false;
        }

        this.address = claimedAddress.getAsLong();

        return true;
    }

    private void loadPersistedState() {
        if (deviceNbt != null) {
            data.clear();

            final byte[] persistedData = deviceNbt.getByteArray(DATA_TAG_NAME);
            data.put(persistedData, 0, Math.min(persistedData.length, data.capacity()));
        }
    }

    private void copyDataToMemory(final long startAddress) {
        final ByteBuffer data = device.getData();
        data.clear();

        try {
            MemoryMaps.store(memoryMap, startAddress, data);
        } catch (final MemoryAccessException e) {
            LOGGER.error(e);
        }
    }

    private void unload() {
        memoryMap = null;
        data = null;
        device = null;
        deviceNbt = null;
        address = null;
    }
}
