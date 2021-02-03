package li.cil.oc2.common.bus.device.item;

import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.item.ItemStack;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public final class HardDriveVMDevice extends AbstractHardDriveVMDevice<ByteBufferBlockDevice, ItemStack> {
    private final int size;
    private final boolean readonly;

    ///////////////////////////////////////////////////////////////////

    public HardDriveVMDevice(final ItemStack identity, final int size, final boolean readonly) {
        super(identity);
        this.size = size;
        this.readonly = readonly;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected int getSize() {
        return size;
    }

    @Override
    protected ByteBufferBlockDevice createBlockDevice() {
        return ByteBufferBlockDevice.create(size, readonly);
    }

    @Override
    protected Optional<InputStream> getSerializationStream(final ByteBufferBlockDevice device) {
        return Optional.of(device.getInputStream());
    }

    @Override
    protected OutputStream getDeserializationStream(final ByteBufferBlockDevice device) {
        return device.getOutputStream();
    }
}
