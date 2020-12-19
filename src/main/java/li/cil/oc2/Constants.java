package li.cil.oc2;

import li.cil.oc2.api.API;

public final class Constants {
    public static final int KILOBYTE = 1024;
    public static final int MEGABYTE = 1024 * KILOBYTE;

    public static final String COMPUTER_BLOCK_NAME = "computer";
    public static final String BUS_CABLE_BLOCK_NAME = "bus_cable";
    public static final String REDSTONE_INTERFACE_BLOCK_NAME = "redstone_interface";
    public static final String SCREEN_BLOCK_NAME = "screen";

    public static final String RAM_NAME = "ram";
    public static final String HDD_NAME = "hdd";

    public static final String HDD_INFO_NBT_TAG_NAME = API.MOD_ID + "hdd";
    public static final String HDD_SIZE_NBT_TAG_NAME = "size";
    public static final String HDD_BASE_NBT_TAG_NAME = "base";
    public static final String HDD_READONLY_NBT_TAG_NAME = "readonly";
}
