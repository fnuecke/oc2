/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.API;
import net.minecraftforge.registries.ObjectHolder;

/**
 * Lists built-in device types for convenience.
 */
public final class DeviceTypes {
    @ObjectHolder(registryName = "item", value=API.MOD_ID+":memory") public static DeviceType MEMORY = null;
    @ObjectHolder(registryName = "item", value=API.MOD_ID+":hard_drive") public static DeviceType HARD_DRIVE = null;
    @ObjectHolder(registryName = "item", value=API.MOD_ID+":flash_memory") public static DeviceType FLASH_MEMORY = null;
    @ObjectHolder(registryName = "item", value=API.MOD_ID+":card") public static DeviceType CARD = null;
    @ObjectHolder(registryName = "item", value=API.MOD_ID+":robot_module") public static DeviceType ROBOT_MODULE = null;
    @ObjectHolder(registryName = "item", value=API.MOD_ID+":floppy") public static DeviceType FLOPPY = null;
    @ObjectHolder(registryName = "item", value=API.MOD_ID+":network_tunnel") public static DeviceType NETWORK_TUNNEL = null;
}
