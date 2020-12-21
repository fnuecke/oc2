package li.cil.oc2;

import li.cil.oc2.api.API;
import net.minecraft.util.Identifier;

public final class Constants {
    public static final int KILOBYTE = 1024;
    public static final int MEGABYTE = 1024 * KILOBYTE;

    public static final Identifier BLOCK_DEVICE_PROVIDER_REGISTRY_NAME = new Identifier(API.MOD_ID, "block_device_providers");
    public static final Identifier ITEM_DEVICE_PROVIDER_REGISTRY_NAME = new Identifier(API.MOD_ID, "item_device_providers");

    ///////////////////////////////////////////////////////////////////

    public static final Identifier COMPUTER_BLOCK_NAME = new Identifier(API.MOD_ID, "computer");
    public static final Identifier BUS_CABLE_BLOCK_NAME = new Identifier(API.MOD_ID, "bus_cable");
    public static final Identifier REDSTONE_INTERFACE_BLOCK_NAME = new Identifier(API.MOD_ID, "redstone_interface");
    public static final Identifier SCREEN_BLOCK_NAME = new Identifier(API.MOD_ID, "screen");

    ///////////////////////////////////////////////////////////////////

    public static final Identifier RAM_ITEM_NAME = new Identifier(API.MOD_ID, "ram");
    public static final Identifier HDD_ITEM_NAME = new Identifier(API.MOD_ID, "hdd");

    ///////////////////////////////////////////////////////////////////

    public static final Identifier COMMON_ITEM_GROUP_NAME = new Identifier(API.MOD_ID, "common");

    ///////////////////////////////////////////////////////////////////

    public static final String HDD_SIZE_NBT_TAG_NAME = "size";
    public static final String HDD_BASE_NBT_TAG_NAME = "base";
    public static final String HDD_READONLY_NBT_TAG_NAME = "readonly";
}
