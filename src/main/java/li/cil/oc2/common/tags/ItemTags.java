package li.cil.oc2.common.tags;

import li.cil.oc2.api.API;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;

public final class ItemTags {
    public static final Tag<Item> DEVICES = tag("devices");
    public static final Tag<Item> DEVICES_MEMORY = tag("devices/memory");
    public static final Tag<Item> DEVICES_HARD_DRIVE = tag("devices/hard_drive");
    public static final Tag<Item> DEVICES_FLASH_MEMORY = tag("devices/flash_memory");
    public static final Tag<Item> DEVICES_CARD = tag("devices/card");
    public static final Tag<Item> DEVICES_ROBOT_MODULE = tag("devices/robot_module");
    public static final Tag<Item> DEVICES_FLOPPY = tag("devices/floppy");

    public static final Tag<Item> TOOL_MATERIALS = tag("tool_materials");
    public static final Tag<Item> TOOL_MATERIAL_WOOD = tag("tool_materials/wood");
    public static final Tag<Item> TOOL_MATERIAL_STONE = tag("tool_materials/stone");
    public static final Tag<Item> TOOL_MATERIAL_IRON = tag("tool_materials/iron");
    public static final Tag<Item> TOOL_MATERIAL_GOLD = tag("tool_materials/gold");
    public static final Tag<Item> TOOL_MATERIAL_DIAMOND = tag("tool_materials/diamond");
    public static final Tag<Item> TOOL_MATERIAL_NETHERITE = tag("tool_materials/netherite");

    public static final Tag<Item> CABLES = tag("cables");
    public static final Tag<Item> WRENCHES = tag("wrenches");
    public static final Tag<Item> DEVICE_NEEDS_REBOOT = tag("device_needs_reboot");

    ///////////////////////////////////////////////////////////////////

    private static Tag<Item> tag(final String name) {
        return TagRegistry.item(new ResourceLocation(API.MOD_ID, name));
    }
}
