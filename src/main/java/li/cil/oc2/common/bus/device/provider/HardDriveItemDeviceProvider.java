package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.Config;
import li.cil.oc2.Constants;
import li.cil.oc2.OpenComputers;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.HardDiskDriveDevice;
import li.cil.oc2.common.bus.device.SparseHardDiskDriveDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;

public class HardDriveItemDeviceProvider extends AbstractItemDeviceProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    public HardDriveItemDeviceProvider() {
        super(OpenComputers.HDD_ITEM.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected LazyOptional<Device> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();

        final CompoundNBT info = stack.getChildTag(Constants.HDD_INFO_NBT_TAG_NAME);
        if (info == null) {
            return LazyOptional.empty();
        }

        final boolean readonly = info.getBoolean(Constants.HDD_READONLY_NBT_TAG_NAME);
        if (info.contains(Constants.HDD_BASE_NBT_TAG_NAME, NBTTagIds.TAG_STRING)) {
            final String baseName = info.getString(Constants.HDD_BASE_NBT_TAG_NAME);

            BlockDevice base = null;

            // TODO Allow registering additional base file systems?
            if (Objects.equals(baseName, "linux")) {
                try {
                    base = ByteBufferBlockDevice.createFromStream(Buildroot.getRootFilesystem(), true);
                } catch (final IOException e) {
                    LOGGER.error(e);
                }
            }

            if (base != null) {
                final BlockDevice baseForClosure = base;
                return LazyOptional.of(() -> new SparseHardDiskDriveDevice(stack, baseForClosure, readonly));
            }
        } else if (info.contains(Constants.HDD_SIZE_NBT_TAG_NAME, NBTTagIds.TAG_INT)) {
            final int size = Math.max(0, Math.min(Config.maxHddSize, info.getInt(Constants.HDD_SIZE_NBT_TAG_NAME)));
            return LazyOptional.of(() -> new HardDiskDriveDevice(stack, size, readonly));
        }

        return LazyOptional.empty();
    }
}
