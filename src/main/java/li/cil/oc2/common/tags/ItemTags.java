package li.cil.oc2.common.tags;

import li.cil.oc2.api.API;
import net.minecraft.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

public final class ItemTags {
    public static final Tags.IOptionalNamedTag<Item> DEVICES = tag("devices");
    public static final Tags.IOptionalNamedTag<Item> DEVICES_MEMORY = tag("devices/memory");
    public static final Tags.IOptionalNamedTag<Item> DEVICES_HARD_DRIVE = tag("devices/hard_drive");
    public static final Tags.IOptionalNamedTag<Item> DEVICES_FLASH_MEMORY = tag("devices/flash_memory");
    public static final Tags.IOptionalNamedTag<Item> DEVICES_CARD = tag("devices/card");
    public static final Tags.IOptionalNamedTag<Item> DEVICES_ROBOT_MODULE = tag("devices/robot_module");
    public static final Tags.IOptionalNamedTag<Item> DEVICES_FLOPPY = tag("devices/floppy");

    public static final Tags.IOptionalNamedTag<Item> TOOL_MATERIALS = tag("tool_materials");
    public static final Tags.IOptionalNamedTag<Item> TOOL_MATERIAL_WOOD = tag("tool_materials/wood");
    public static final Tags.IOptionalNamedTag<Item> TOOL_MATERIAL_STONE = tag("tool_materials/stone");
    public static final Tags.IOptionalNamedTag<Item> TOOL_MATERIAL_IRON = tag("tool_materials/iron");
    public static final Tags.IOptionalNamedTag<Item> TOOL_MATERIAL_GOLD = tag("tool_materials/gold");
    public static final Tags.IOptionalNamedTag<Item> TOOL_MATERIAL_DIAMOND = tag("tool_materials/diamond");
    public static final Tags.IOptionalNamedTag<Item> TOOL_MATERIAL_NETHERITE = tag("tool_materials/netherite");

    public static final Tags.IOptionalNamedTag<Item> CABLES = tag("cables");
    public static final Tags.IOptionalNamedTag<Item> WRENCHES = tag("wrenches");
    public static final Tags.IOptionalNamedTag<Item> DEVICE_NEEDS_REBOOT = tag("device_needs_reboot");

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    ///////////////////////////////////////////////////////////////////

    private static Tags.IOptionalNamedTag<Item> tag(final String name) {
        return net.minecraft.tags.ItemTags.createOptional(new ResourceLocation(API.MOD_ID, name));
    }
}
