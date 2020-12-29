package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.API;
import net.minecraftforge.registries.ObjectHolder;

/**
 * Lists built-in device types for convenience.
 */
@ObjectHolder(API.MOD_ID)
public final class DeviceTypes {
    @ObjectHolder("memory") public static DeviceType MEMORY = null;
    @ObjectHolder("hard_drive") public static DeviceType HARD_DRIVE = null;
    @ObjectHolder("flash_memory") public static DeviceType FLASH_MEMORY = null;
    @ObjectHolder("card") public static DeviceType CARD = null;
}
