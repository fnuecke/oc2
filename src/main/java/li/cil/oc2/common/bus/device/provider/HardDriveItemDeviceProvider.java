package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.Config;
import li.cil.oc2.Constants;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.HardDiskDriveDevice;
import li.cil.oc2.common.bus.device.SparseHardDiskDriveDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.init.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
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
        super(Items.HDD_ITEM);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();

        final CompoundNBT info = ItemStackUtils.getModDataTag(stack);
        if (info == null) {
            return Optional.empty();
        }

        final boolean readonly = info.getBoolean(Constants.HDD_READONLY_NBT_TAG_NAME);
        if (info.contains(Constants.HDD_BASE_NBT_TAG_NAME, NBTTagIds.TAG_STRING)) {
            final String baseName = info.getString(Constants.HDD_BASE_NBT_TAG_NAME);

            final BlockDevice base = getBaseBlockDevice(baseName);

            if (base != null) {
                return Optional.of(new SparseHardDiskDriveDevice(stack, base, readonly));
            }
        } else if (info.contains(Constants.HDD_SIZE_NBT_TAG_NAME, NBTTagIds.TAG_INT)) {
            final int size = Math.max(0, Math.min(Config.maxHddSize, info.getInt(Constants.HDD_SIZE_NBT_TAG_NAME)));
            return Optional.of(new HardDiskDriveDevice(stack, size, readonly));
        }

        return Optional.empty();
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
