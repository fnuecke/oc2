package li.cil.oc2.common;

import li.cil.oc2.api.API;
import net.minecraft.core.Direction;

public final class Constants {
    public static final int KILOBYTE = 1024;
    public static final int MEGABYTE = 1024 * KILOBYTE;
    public static final int GIGABYTE = 1024 * MEGABYTE;

    public static final int CPU_FREQUENCY = 25_000_000;

    public static final int TICK_SECONDS = 20;

    public static final Direction[] DIRECTIONS = Direction.values();
    public static final Direction.Axis[] AXES = Direction.Axis.values();
    public static final int BLOCK_FACE_COUNT = DIRECTIONS.length;

    ///////////////////////////////////////////////////////////////////

    public static final String BLOCK_ENTITY_TAG_NAME_IN_ITEM = "BlockEntityTag";
    public static final String MOD_TAG_NAME = API.MOD_ID;
    public static final String ITEMS_TAG_NAME = "items";
    public static final String ENERGY_TAG_NAME = "energy";

    ///////////////////////////////////////////////////////////////////

    public static final String TOOLTIP_DESCRIPTION_SUFFIX = ".desc";
    public static final String TOOLTIP_DEVICE_NEEDS_REBOOT = "tooltip.oc2.device_needs_reboot";
    public static final String TOOLTIP_FLASH_MEMORY_MISSING = "tooltip.oc2.flash_memory_missing";
    public static final String TOOLTIP_MEMORY_MISSING = "tooltip.oc2.memory_missing";
    public static final String TOOLTIP_HARD_DRIVE_MISSING = "tooltip.oc2.hard_drive_missing";
    public static final String TOOLTIP_ENERGY = "tooltip.oc2.energy";
    public static final String TOOLTIP_ENERGY_CONSUMPTION = "tooltip.oc2.energyConsumption";
    public static final String TOOLTIP_CONFIRM = "tooltip.oc2.confirm";
    public static final String TOOLTIP_CANCEL = "tooltip.oc2.cancel";

    ///////////////////////////////////////////////////////////////////

    public static final String COMPUTER_SCREEN_CAPTURE_INPUT_CAPTION = "gui.oc2.computer.capture_input.capt";
    public static final String COMPUTER_SCREEN_CAPTURE_INPUT_DESCRIPTION = "gui.oc2.computer.capture_input.desc";
    public static final String COMPUTER_SCREEN_POWER_CAPTION = "gui.oc2.computer.power.capt";
    public static final String COMPUTER_SCREEN_POWER_DESCRIPTION = "gui.oc2.computer.power.desc";
    public static final String COMPUTER_ERROR_UNKNOWN = "gui.oc2.computer.error.unknown";
    public static final String COMPUTER_ERROR_MISSING_FIRMWARE = "gui.oc2.computer.error.missing_firmware";
    public static final String COMPUTER_ERROR_INSUFFICIENT_MEMORY = "gui.oc2.computer.error.insufficient_memory";
    public static final String COMPUTER_BUS_STATE_INCOMPLETE = "gui.oc2.computer.bus_state.incomplete";
    public static final String COMPUTER_BUS_STATE_TOO_COMPLEX = "gui.oc2.computer.bus_state.too_complex";
    public static final String COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS = "gui.oc2.computer.bus_state.multiple_controllers";
    public static final String COMPUTER_ERROR_NOT_ENOUGH_ENERGY = "gui.oc2.computer.error.not_enough_energy";

    ///////////////////////////////////////////////////////////////////

    public static final String CONNECTOR_ERROR_FULL = "message.oc2.connector.error.full";
    public static final String CONNECTOR_ERROR_TOO_FAR = "message.oc2.connector.error.too_far";
    public static final String CONNECTOR_ERROR_OBSTRUCTED = "message.oc2.connector.error.obstructed";
}
