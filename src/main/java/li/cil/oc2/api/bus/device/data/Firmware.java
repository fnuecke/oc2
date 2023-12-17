/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.data;

import li.cil.sedna.api.memory.MemoryMap;
import net.minecraft.network.chat.Component;

/**
 * Implementations of this interface that are registered with the registry for
 * this type can be used as executable firmware for flash memory devices.
 * <p>
 * This is used for the built-in OpenSBI firmware and Linux kernel, for example.
 * <p>
 * To make use of registered implementations, a flash memory item with the
 * string tag {@code oc2.firmware} referencing the implementation's registry name
 * must be created. For example, if the implementation's registry name is
 * {@code my_mod:my_firmware}:
 * <pre>
 * /give &#64;p oc2:flash_memory{oc2:{firmware:"my_mod:my_firmware"}}
 * </pre>
 */
public interface Firmware {
    /**
     * Runs this firmware.
     * <p>
     * This will usually load machine code into memory at the specified start address.
     * <p>
     * Typically, only returns {@code false} when there was not enough memory to fit the firmware.
     *
     * @param memory       access to the memory map of the machine.
     * @param startAddress the memory address where execution will commence.
     * @return {@code true} if the firmware was loaded successfully; {@code false} otherwise.
     */
    boolean run(final MemoryMap memory, final long startAddress);

    /**
     * The display name of this firmware. May be shown in the tooltip of item devices
     * using this firmware.
     *
     * @return the display name of this firmware.
     */
    Component getDisplayName();
}
