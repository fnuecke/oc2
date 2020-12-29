package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BaseBlockDevice;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class BuildrootRootFileSystem extends ForgeRegistryEntry<BaseBlockDevice> implements BaseBlockDevice {
    private static final Logger LOGGER = LogManager.getLogger();

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

    @Override
    public BlockDevice get() {
        return ROOT_FS;
    }

    @Override
    public ITextComponent getName() {
        return new StringTextComponent("Linux");
    }
}
