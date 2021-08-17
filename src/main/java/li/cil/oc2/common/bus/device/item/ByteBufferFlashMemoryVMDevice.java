package li.cil.oc2.common.bus.device.item;

import com.google.common.eventbus.Subscribe;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.FirmwareLoader;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.device.flash.FlashMemoryDevice;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;

import java.nio.ByteBuffer;

@SuppressWarnings("UnstableApiUsage")
public final class ByteBufferFlashMemoryVMDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice, FirmwareLoader {
    public static final String DATA_TAG_NAME = "data";

    ///////////////////////////////////////////////////////////////

    private final int size;
    private MemoryMap memoryMap;
    private ByteBuffer data;
    private FlashMemoryDevice device;

    ///////////////////////////////////////////////////////////////

    // Online persisted data.
    private CompoundNBT deviceTag;
    private final OptionalAddress address = new OptionalAddress();

    ///////////////////////////////////////////////////////////////

    public ByteBufferFlashMemoryVMDevice(final ItemStack identity, final int size) {
        super(identity);
        this.size = size;
    }

    ///////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        if (!address.claim(context, device)) {
            return VMDeviceLoadResult.fail();
        }

        loadPersistedState();

        memoryMap = context.getMemoryMap();

        context.getEventBus().register(this);

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        suspend();
        deviceTag = null;
        address.clear();
    }

    @Override
    public void suspend() {
        memoryMap = null;
        data = null;
        device = null;
    }

    @Subscribe
    public void handleInitializingEvent(final VMInitializingEvent event) {
        copyDataToMemory(event.getProgramStartAddress());
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT tag = new CompoundNBT();

        if (device != null) {
            tag.putByteArray(DATA_TAG_NAME, device.getData().array());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundNBT tag) {
        final byte[] data = tag.getByteArray(DATA_TAG_NAME);
        final ByteBuffer bufferData = device.getData();
        bufferData.clear();
        bufferData.put(data, 0, Math.min(bufferData.limit(), data.length));
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

    private void loadPersistedState() {
        if (deviceTag != null) {
            data.clear();

            final byte[] persistedData = deviceTag.getByteArray(DATA_TAG_NAME);
            data.put(persistedData, 0, Math.min(persistedData.length, data.capacity()));
        }
    }

    private void copyDataToMemory(final long startAddress) {
        final ByteBuffer data = device.getData();
        data.clear();

        try {
            MemoryMaps.store(memoryMap, startAddress, data);
        } catch (final MemoryAccessException e) {
            throw new VMInitializationException(new TranslationTextComponent(Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY));
        }
    }
}
