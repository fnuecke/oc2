package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.API;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder(API.MOD_ID)
public final class DeviceTypes {
    @ObjectHolder("eeprom") public static DeviceType EEPROM = null;
    @ObjectHolder("memory") public static DeviceType MEMORY = null;
    @ObjectHolder("hard_drive") public static DeviceType HARD_DRIVE = null;
    @ObjectHolder("card") public static DeviceType CARD = null;
}
