/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;

public final class BlockDeviceDataClientView implements BlockDeviceDataResource {
    private final ResourceLocation location;
    private final long capacity;
    private final Component displayName;
    @Nullable
    private final DyeColor color;

    public BlockDeviceDataClientView(final ResourceLocation location, final long capacity, final Component displayName, @Nullable final DyeColor color) {
        this.location = location;
        this.capacity = capacity;
        this.displayName = displayName;
        this.color = color;
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }

    @Override
    public BlockDevice getBlockDevice() {
        throw new IllegalStateException("The contents of [" + location + "] are only available on the server.");
    }

    @Override
    public long getCapacity() {
        return capacity;
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
