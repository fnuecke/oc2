/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import li.cil.oc2.api.API;
import net.minecraft.core.Direction;

import static li.cil.oc2.common.util.TranslationUtils.key;

public final class Constants {
    public static final int KILOBYTE = 1024;
    public static final int MEGABYTE = 1024 * KILOBYTE;
    public static final int GIGABYTE = 1024 * MEGABYTE;

    public static final int PAGE_SIZE = 4 * 1024;

    public static final int FLASH_MEMORY_SIZE = 8 * KILOBYTE;

    public static final int MEMORY_SMALL_SIZE = 2 * MEGABYTE;
    public static final int MEMORY_MEDIUM_SIZE = 4 * MEGABYTE;
    public static final int MEMORY_LARGE_SIZE = 8 * MEGABYTE;

    public static final int HARD_DRIVE_SMALL_SIZE = 2 * MEGABYTE;
    public static final int HARD_DRIVE_MEDIUM_SIZE = 4 * MEGABYTE;
    public static final int HARD_DRIVE_LARGE_SIZE = 8 * MEGABYTE;

    public static final int FLOPPY_SIZE = 512 * KILOBYTE;

    // The largest binary payload a VM accepts from a guest in one RPC call.
    public static final int RPC_MAX_PAYLOAD_SIZE = 512 * KILOBYTE;

    // Note: these mean consumed *guest* memory, so kept low.
    public static final int VIRTIO_BLOCK_QUEUE_SIZE = 32;
    public static final int VIRTIO_FILESYSTEM_QUEUE_SIZE = 32;
    public static final int VIRTIO_NETWORK_QUEUE_SIZE = 64;
    public static final int VIRTIO_INPUT_QUEUE_SIZE = 8;

    public static final int SECONDS_TO_TICKS = 20;

    public static final Direction[] DIRECTIONS = Direction.values();
    public static final Direction.Axis[] AXES = Direction.Axis.values();
    public static final int BLOCK_FACE_COUNT = DIRECTIONS.length;

    // --------------------------------------------------------------------- //

    public static final String MOD_TAG_NAME = API.MOD_ID;
    public static final String ITEMS_TAG_NAME = "items";
    public static final String ENERGY_TAG_NAME = "energy";

    // --------------------------------------------------------------------- //

    public static final String TOOLTIP_DESCRIPTION_SUFFIX = ".desc";
    public static final String TOOLTIP_DEVICE_NEEDS_REBOOT = key("tooltip.{mod}.device_needs_reboot");
    public static final String TOOLTIP_CAN_BE_CONFIGURED = key("tooltip.{mod}.can_be_configured");
    public static final String TOOLTIP_ENERGY = key("tooltip.{mod}.energy");
    public static final String TOOLTIP_ENERGY_CONSUMPTION = key("tooltip.{mod}.energyConsumption");
    public static final String TOOLTIP_DATA_CORRUPTED = key("tooltip.{mod}.data_corrupted");
    public static final String TOOLTIP_DATA_CORRUPTED_HINT = key("tooltip.{mod}.data_corrupted.hint");
    public static final String TOOLTIP_DATA_INCONSISTENT = key("tooltip.{mod}.data_inconsistent");
    public static final String TOOLTIP_DATA_INCONSISTENT_HINT = key("tooltip.{mod}.data_inconsistent.hint");
    public static final String TOOLTIP_INTERNET_DISABLED = key("tooltip.{mod}.internet_disabled");
    public static final String TOOLTIP_CONFIRM = key("tooltip.{mod}.confirm");
    public static final String TOOLTIP_CANCEL = key("tooltip.{mod}.cancel");

    // --------------------------------------------------------------------- //

    public static final String TERMINAL_CAPTURE_INPUT_CAPTION = key("gui.{mod}.computer.capture_input.capt");
    public static final String TERMINAL_CAPTURE_INPUT_DESCRIPTION = key("gui.{mod}.computer.capture_input.desc");
    public static final String COMPUTER_SCREEN_POWER_CAPTION = key("gui.{mod}.computer.power.capt");
    public static final String COMPUTER_SCREEN_POWER_DESCRIPTION = key("gui.{mod}.computer.power.desc");
    public static final String COMPUTER_ERROR_UNKNOWN = key("gui.{mod}.computer.error.unknown");
    public static final String COMPUTER_ERROR_MISSING_CPU = key("gui.{mod}.computer.error.missing_cpu");
    public static final String COMPUTER_ERROR_MISSING_FIRMWARE = key("gui.{mod}.computer.error.missing_firmware");
    public static final String COMPUTER_ERROR_INSUFFICIENT_MEMORY = key("gui.{mod}.computer.error.insufficient_memory");
    public static final String COMPUTER_ERROR_DEVICE_DOES_NOT_FIT = key("gui.{mod}.computer.error.device_does_not_fit");
    public static final String COMPUTER_ERROR_STORAGE_CORRUPTED = key("gui.{mod}.computer.error.storage_corrupted");
    public static final String COMPUTER_ERROR_STORAGE_FULL = key("gui.{mod}.computer.error.storage_full");
    public static final String COMPUTER_ERROR_MEMORY_CORRUPTED = key("gui.{mod}.computer.error.memory_corrupted");
    public static final String COMPUTER_ERROR_STATE_LOST = key("gui.{mod}.computer.error.state_lost");
    public static final String COMPUTER_ERROR_STORAGE_INCONSISTENT = key("gui.{mod}.computer.error.storage_inconsistent");
    public static final String COMPUTER_BUS_STATE_INCOMPLETE = key("gui.{mod}.computer.bus_state.incomplete");
    public static final String COMPUTER_BUS_STATE_TOO_COMPLEX = key("gui.{mod}.computer.bus_state.too_complex");
    public static final String COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS = key("gui.{mod}.computer.bus_state.multiple_controllers");
    public static final String COMPUTER_ERROR_NOT_ENOUGH_ENERGY = key("gui.{mod}.computer.error.not_enough_energy");
    public static final String MACHINE_OPEN_INVENTORY_CAPTION = key("gui.{mod}.machine.open_inventory.capt");
    public static final String MACHINE_OPEN_TERMINAL_CAPTION = key("gui.{mod}.machine.open_terminal.capt");

    // --------------------------------------------------------------------- //

    public static final String CONNECTOR_ERROR_FULL = key("message.{mod}.connector.error.full");
    public static final String CONNECTOR_ERROR_TOO_FAR = key("message.{mod}.connector.error.too_far");
    public static final String CONNECTOR_ERROR_OBSTRUCTED = key("message.{mod}.connector.error.obstructed");
}
