package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.Config;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.HardDriveDevice;
import li.cil.oc2.common.bus.device.SparseHardDriveDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.init.Items;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class HardDriveItemDeviceProvider extends AbstractItemDeviceProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final ByteBufferBlockDevice ROOT_FS;

    static {
        ByteBufferBlockDevice rootfs;
        try {
            rootfs = ByteBufferBlockDevice.createFromStream(Buildroot.getRootFilesystem(), true);
        } catch (final IOException e) {
            LOGGER.error(e);
            rootfs = ByteBufferBlockDevice.create(0, true);
        }
        ROOT_FS = rootfs;
    }

    ///////////////////////////////////////////////////////////////////

    public HardDriveItemDeviceProvider() {
        super(Items.HARD_DRIVE_ITEM);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();

        final CompoundNBT info = ItemStackUtils.getModDataTag(stack);
        if (info == null) {
            return Optional.empty();
        }

        final boolean readonly = HardDriveItem.isReadonly(stack);
        final String baseBlockDevice = HardDriveItem.getBaseBlockDevice(stack);
        if (baseBlockDevice != null) {
            final BlockDevice base = getBaseBlockDevice(baseBlockDevice);
            if (base != null) {
                return Optional.of(new SparseHardDriveDevice(stack, base, readonly));
            }
        }

        final int size = MathHelper.clamp(HardDriveItem.getCapacity(stack), 0, Config.maxHardDriveSize);
        return Optional.of(new HardDriveDevice(stack, size, readonly));
    }

    @Override
    protected Optional<DeviceType> getItemDeviceType(final ItemDeviceQuery query) {
        return Optional.of(DeviceTypes.HARD_DRIVE);
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    private static BlockDevice getBaseBlockDevice(final String name) {
        // TODO Allow registering additional base file systems.
        if (Objects.equals(name, "linux")) {
            return ROOT_FS;
        }

        return null;
    }
}
