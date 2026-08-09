/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class BuildrootBlockDeviceData implements BlockDeviceData {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final ByteBufferBlockDevice INSTANCE;

    static {
        ByteBufferBlockDevice instance;
        try {
            instance = ByteBufferBlockDevice.createFromStream(Buildroot.getRootFilesystem(), true);
        } catch (final IOException e) {
            LOGGER.error(e);
            instance = ByteBufferBlockDevice.create(0, true);
        }
        INSTANCE = instance;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public BlockDevice getBlockDevice() {
        return INSTANCE;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Sedna Linux");
    }
}
