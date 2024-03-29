/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

public final class ResourceBlockDeviceData implements IForgeRegistryEntry<BlockDeviceData>, BlockDeviceData, AutoCloseable {
    private final ResourceLocation location;
    private final String name;
    private final BlockDevice blockDevice;

    public ResourceBlockDeviceData(final ResourceManager resourceManager, final ResourceLocation location, final String name) throws IOException {
        this.location = location;
        this.name = name;
        final InputStream stream = resourceManager.getResource(location).getInputStream();
        this.blockDevice = ByteBufferBlockDevice.createFromStream(stream, true);
    }

    @Override
    public BlockDeviceData setRegistryName(final ResourceLocation name) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName() {
        return location;
    }

    @Override
    public Class<BlockDeviceData> getRegistryType() {
        return BlockDeviceData.class;
    }

    @Override
    public void close() throws Exception {
        blockDevice.close();
    }

    @Override
    public BlockDevice getBlockDevice() {
        return blockDevice;
    }

    @Override
    public Component getDisplayName() {
        return new TextComponent(name);
    }
}
