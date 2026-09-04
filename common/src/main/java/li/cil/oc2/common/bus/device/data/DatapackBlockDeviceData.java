/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

public final class DatapackBlockDeviceData implements BlockDeviceData, AutoCloseable {
    private final ResourceLocation location;
    private final String name;
    @Nullable
    private final DyeColor color;
    private final BlockDevice blockDevice;

    public DatapackBlockDeviceData(final ResourceManager resourceManager, final ResourceLocation location, final String name, @Nullable final DyeColor color) throws IOException {
        this.location = location;
        this.name = name;
        this.color = color;
        final InputStream stream = resourceManager.getResourceOrThrow(location).open();
        this.blockDevice = ByteBufferBlockDevice.createFromStream(stream, true);
    }

    public ResourceLocation getLocation() {
        return location;
    }

    @Override
    public void close() throws IOException {
        blockDevice.close();
    }

    @Override
    public BlockDevice getBlockDevice() {
        return blockDevice;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(name);
    }

    @Nullable
    @Override
    public DyeColor getColor() {
        return color;
    }
}
