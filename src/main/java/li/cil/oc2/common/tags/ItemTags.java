package li.cil.oc2.common.tags;

import li.cil.oc2.api.API;
import net.minecraft.item.Item;
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

    public static final Tags.IOptionalNamedTag<Item> BLOCK_OPERATIONS_MODULE_MATERIAL = tag("block_operations_module_material");
    public static final Tags.IOptionalNamedTag<Item> BLOCK_OPERATIONS_MODULE_MATERIAL_WOOD = tag("block_operations_module_material/wood");
    public static final Tags.IOptionalNamedTag<Item> BLOCK_OPERATIONS_MODULE_MATERIAL_STONE = tag("block_operations_module_material/stone");
    public static final Tags.IOptionalNamedTag<Item> BLOCK_OPERATIONS_MODULE_MATERIAL_IRON = tag("block_operations_module_material/iron");
    public static final Tags.IOptionalNamedTag<Item> BLOCK_OPERATIONS_MODULE_MATERIAL_GOLD = tag("block_operations_module_material/gold");
    public static final Tags.IOptionalNamedTag<Item> BLOCK_OPERATIONS_MODULE_MATERIAL_DIAMOND = tag("block_operations_module_material/diamond");
    public static final Tags.IOptionalNamedTag<Item> BLOCK_OPERATIONS_MODULE_MATERIAL_NETHERITE = tag("block_operations_module_material/netherite");

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
