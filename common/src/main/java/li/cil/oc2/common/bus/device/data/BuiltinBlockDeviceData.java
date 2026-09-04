/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import com.google.common.base.Suppliers;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public final class BuiltinBlockDeviceData implements BlockDeviceData {
    private static final Logger LOGGER = LogManager.getLogger();

    // --------------------------------------------------------------------- //

    private final Supplier<BlockDevice> blockDevice;
    private final Component displayName;
    private final DyeColor color;

    // --------------------------------------------------------------------- //

    public BuiltinBlockDeviceData(final Supplier<BlockDevice> blockDevice, final String name, final DyeColor color) {
        this.blockDevice = blockDevice;
        this.displayName = Component.literal(name);
        this.color = color;
    }

    public static Supplier<BlockDevice> readOnce(final Supplier<InputStream> source) {
        return Suppliers.memoize(() -> {
            try (InputStream stream = source.get()) {
                return ByteBufferBlockDevice.createFromStream(stream, true);
            } catch (final IOException e) {
                LOGGER.error(e);
                return ByteBufferBlockDevice.create(0, true);
            }
        });
    }

    public static Supplier<BlockDevice> readEachTime(final Supplier<byte[]> source) {
        return () -> ByteBufferBlockDevice.wrap(ByteBuffer.wrap(source.get()), true);
    }

    // --------------------------------------------------------------------- //

    @Override
    public BlockDevice getBlockDevice() {
        return blockDevice.get();
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }

    @Nullable
    @Override
    public DyeColor getColor() {
        return color;
    }
}
