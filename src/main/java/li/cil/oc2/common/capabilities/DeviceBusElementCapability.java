package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.bus.AbstractDeviceBusElement;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Collection;
import java.util.Optional;

public final class DeviceBusElementCapability {
    public static void register() {
        CapabilityManager.INSTANCE.register(DeviceBusElement.class, new NullStorage<>(), Implementation::new);
    }

    ///////////////////////////////////////////////////////////////////

    private static final class Implementation extends AbstractDeviceBusElement {
        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return Optional.empty();
        }
    }
}
