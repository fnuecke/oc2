/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.DeviceType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class DeviceTypeImpl implements DeviceType {
    private final TagKey<Item> tag;
    private final ResourceLocation icon;
    private final Component name;

    public DeviceTypeImpl(final TagKey<Item> tag, final ResourceLocation icon, final Component name) {
        this.tag = tag;
        this.icon = icon;
        this.name = name;
    }

    @Override
    public TagKey<Item> getTag() {
        return tag;
    }

    @Override
    public ResourceLocation getBackgroundIcon() {
        return icon;
    }

    @Override
    public Component getName() {
        return name;
    }
}
