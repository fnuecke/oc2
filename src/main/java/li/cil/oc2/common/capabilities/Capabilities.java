package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusElement;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public final class Capabilities {
    @CapabilityInject(DeviceBus.class) @SuppressWarnings("FieldMayBeFinal")
    public static Capability<DeviceBusElement> DEVICE_BUS_ELEMENT_CAPABILITY = null;

}
