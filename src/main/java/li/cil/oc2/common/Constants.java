package li.cil.oc2.common;

import li.cil.oc2.api.API;
import net.minecraft.core.Direction;

import static li.cil.oc2.common.util.TranslationUtils.key;

public final class Constants {
    public static final int KILOBYTE = 1024;
    public static final int MEGABYTE = 1024 * KILOBYTE;
    public static final int GIGABYTE = 1024 * MEGABYTE;

    public static final int PAGE_SIZE = 4 * 1024;

    public static final int CPU_FREQUENCY = 25_000_000;

    public static final int SECONDS_TO_TICKS = 20;

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
    public static final String TOOLTIP_DEVICE_NEEDS_REBOOT = key("tooltip.{mod}.device_needs_reboot");
    public static final String TOOLTIP_ENERGY = key("tooltip.{mod}.energy");
    public static final String TOOLTIP_ENERGY_CONSUMPTION = key("tooltip.{mod}.energyConsumption");
    public static final String TOOLTIP_CONFIRM = key("tooltip.{mod}.confirm");
    public static final String TOOLTIP_CANCEL = key("tooltip.{mod}.cancel");

    ///////////////////////////////////////////////////////////////////

    public static final String TERMINAL_CAPTURE_INPUT_CAPTION = key("gui.{mod}.computer.capture_input.capt");
    public static final String TERMINAL_CAPTURE_INPUT_DESCRIPTION = key("gui.{mod}.computer.capture_input.desc");
    public static final String COMPUTER_SCREEN_POWER_CAPTION = key("gui.{mod}.computer.power.capt");
    public static final String COMPUTER_SCREEN_POWER_DESCRIPTION = key("gui.{mod}.computer.power.desc");
    public static final String COMPUTER_ERROR_UNKNOWN = key("gui.{mod}.computer.error.unknown");
    public static final String COMPUTER_ERROR_MISSING_FIRMWARE = key("gui.{mod}.computer.error.missing_firmware");
    public static final String COMPUTER_ERROR_INSUFFICIENT_MEMORY = key("gui.{mod}.computer.error.insufficient_memory");
    public static final String COMPUTER_BUS_STATE_INCOMPLETE = key("gui.{mod}.computer.bus_state.incomplete");
    public static final String COMPUTER_BUS_STATE_TOO_COMPLEX = key("gui.{mod}.computer.bus_state.too_complex");
    public static final String COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS = key("gui.{mod}.computer.bus_state.multiple_controllers");
    public static final String COMPUTER_ERROR_NOT_ENOUGH_ENERGY = key("gui.{mod}.computer.error.not_enough_energy");
    public static final String MACHINE_OPEN_INVENTORY_CAPTION = key("gui.{mod}.machine.open_inventory.capt");
    public static final String MACHINE_OPEN_TERMINAL_CAPTION = key("gui.{mod}.machine.open_terminal.capt");

    ///////////////////////////////////////////////////////////////////

    public static final String CONNECTOR_ERROR_FULL = key("message.{mod}.connector.error.full");
    public static final String CONNECTOR_ERROR_TOO_FAR = key("message.{mod}.connector.error.too_far");
    public static final String CONNECTOR_ERROR_OBSTRUCTED = key("message.{mod}.connector.error.obstructed");
}
