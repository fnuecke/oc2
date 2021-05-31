package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.common.util.Location;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.ThrottledSoundEmitter;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public final class HardDriveVMDevice extends AbstractBlockDeviceVMDevice<ByteBufferBlockDevice, ItemStack> {
    private final int size;
    private final boolean readonly;
    private final ThrottledSoundEmitter soundEmitter;

    ///////////////////////////////////////////////////////////////////

    public HardDriveVMDevice(final ItemStack identity, final int size, final boolean readonly, final Supplier<Optional<Location>> location) {
        super(identity);
        this.size = size;
        this.readonly = readonly;
        this.soundEmitter = new ThrottledSoundEmitter(location, SoundEvents.HDD_ACCESS)
                .withMinInterval(Duration.ofSeconds(1));
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

    @Override
    protected void handleDataAccess() {
        soundEmitter.play();
    }
}
