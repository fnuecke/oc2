/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

public final class FirmwareBlockDeviceData implements BlockDeviceData {
    private static final Logger LOGGER = LogManager.getLogger();

    // --------------------------------------------------------------------- //

    private final BlockDevice blockDevice;
    private final Component displayName;
    private final DyeColor color;

    // --------------------------------------------------------------------- //

    public FirmwareBlockDeviceData(final Supplier<InputStream> source, final String name, final DyeColor color) {
        BlockDevice device;
        try (InputStream stream = source.get()) {
            device = ByteBufferBlockDevice.createFromStream(stream, true);
        } catch (final IOException e) {
            LOGGER.error(e);
            device = ByteBufferBlockDevice.create(0, true);
        }

        this.blockDevice = device;
        this.displayName = Component.literal(name);
        this.color = color;
    }

    // --------------------------------------------------------------------- //

    @Override
    public BlockDevice getBlockDevice() {
        return blockDevice;
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public DyeColor getColor() {
        return color;
    }
}
