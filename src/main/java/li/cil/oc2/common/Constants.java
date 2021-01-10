package li.cil.oc2.common;

import net.minecraft.util.Direction;

public final class Constants {
    public static final int KILOBYTE = 1024;
    public static final int MEGABYTE = 1024 * KILOBYTE;
    public static final int GIGABYTE = 1024 * MEGABYTE;

    public static final int TICK_SECONDS = 20;

    public static final Direction[] DIRECTIONS = Direction.values();
    public static final Direction.Axis[] AXES = Direction.Axis.values();
    public static final int BLOCK_FACE_COUNT = DIRECTIONS.length;

    ///////////////////////////////////////////////////////////////////

    public static final String BLOCK_ENTITY_TAG_NAME_IN_ITEM = "BlockEntityTag";
    public static final String BLOCK_ENTITY_INVENTORY_TAG_NAME = "items";

    ///////////////////////////////////////////////////////////////////

    public static final String COMPUTER_BLOCK_NAME = "computer";
    public static final String BUS_CABLE_BLOCK_NAME = "bus_cable";
    public static final String NETWORK_CONNECTOR_BLOCK_NAME = "network_connector";
    public static final String REDSTONE_INTERFACE_BLOCK_NAME = "redstone_interface";

    ///////////////////////////////////////////////////////////////////

    public static final String WRENCH_ITEM_NAME = "wrench";

    public static final String BUS_INTERFACE_ITEM_NAME = "bus_interface";
    public static final String NETWORK_CABLE_NAME = "network_cable";

    public static final String FLASH_MEMORY_ITEM_NAME = "flash_memory";
    public static final String MEMORY_ITEM_NAME = "memory";
    public static final String HARD_DRIVE_ITEM_NAME = "hard_drive";
    public static final String REDSTONE_INTERFACE_CARD_NAME = "redstone_interface_card";
    public static final String NETWORK_INTERFACE_CARD_NAME = "network_interface_card";

    ///////////////////////////////////////////////////////////////////

    public static final String CONFIG_MAX_ALLOCATED_MEMORY = "config.oc2.maxAllocatedMemory";
    public static final String CONFIG_MAX_MEMORY_SIZE = "config.oc2.maxMemorySize";
    public static final String CONFIG_MAX_HARD_DRIVE_SIZE = "config.oc2.maxHardDriveSize";
    public static final String CONFIG_MAX_FLASH_MEMORY_SIZE = "config.oc2.maxFlashMemorySize";

    ///////////////////////////////////////////////////////////////////

    public static final String COMPUTER_SCREEN_CAPTURE_INPUT_CAPTION = "gui.oc2.computer.capture_input.capt";
    public static final String COMPUTER_SCREEN_CAPTURE_INPUT_DESCRIPTION = "gui.oc2.computer.capture_input.desc";
    public static final String COMPUTER_SCREEN_POWER_CAPTION = "gui.oc2.computer.power.capt";
    public static final String COMPUTER_SCREEN_POWER_DESCRIPTION = "gui.oc2.computer.power.desc";
    public static final String COMPUTER_BOOT_ERROR_UNKNOWN = "gui.oc2.computer.boot_error.unknown";
    public static final String COMPUTER_BOOT_ERROR_NO_MEMORY = "gui.oc2.computer.boot_error.no_memory";
    public static final String COMPUTER_BUS_STATE_INCOMPLETE = "gui.oc2.computer.bus_state.incomplete";
    public static final String COMPUTER_BUS_STATE_TOO_COMPLEX = "gui.oc2.computer.bus_state.too_complex";
    public static final String COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS = "gui.oc2.computer.bus_state.multiple_controllers";

    ///////////////////////////////////////////////////////////////////

    public static final String CONNECTOR_ERROR_FULL = "message.oc2.connector.error.full";
    public static final String CONNECTOR_ERROR_TOO_FAR = "message.oc2.connector.error.too_far";
    public static final String CONNECTOR_ERROR_OBSTRUCTED = "message.oc2.connector.error.obstructed";
}
