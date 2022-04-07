/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.tags;

import li.cil.oc2.api.API;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ItemTags {
    public static final TagKey<Item> DEVICES = tag("devices");
    public static final TagKey<Item> DEVICES_MEMORY = tag("devices/memory");
    public static final TagKey<Item> DEVICES_HARD_DRIVE = tag("devices/hard_drive");
    public static final TagKey<Item> DEVICES_FLASH_MEMORY = tag("devices/flash_memory");
    public static final TagKey<Item> DEVICES_CARD = tag("devices/card");
    public static final TagKey<Item> DEVICES_ROBOT_MODULE = tag("devices/robot_module");
    public static final TagKey<Item> DEVICES_FLOPPY = tag("devices/floppy");
    public static final TagKey<Item> DEVICES_NETWORK_TUNNEL = tag("devices/network_tunnel");

    public static final TagKey<Item> CABLES = tag("cables");
    public static final TagKey<Item> WRENCHES = tag("wrenches");
    public static final TagKey<Item> DEVICE_NEEDS_REBOOT = tag("device_needs_reboot");

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    ///////////////////////////////////////////////////////////////////

    private static TagKey<Item> tag(final String name) {
        return TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(API.MOD_ID, name));
    }
}
