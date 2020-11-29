package li.cil.oc2.common.util;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class TileEntities {
    private static final Logger LOGGER = LogManager.getLogger();

    // We map to suppliers for tests, where assigned capability statics may change.
    private static final Map<Class<?>, Supplier<Capability<?>>> CAPABILITIES = Util.make(() -> {
        final HashMap<Class<?>, Supplier<Capability<?>>> map = new HashMap<>();

        map.put(DeviceBusElement.class, () -> Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY);

        return map;
    });

    public static <T> Optional<T> getInterfaceForSide(final TileEntity tileEntity, final Class<T> type, @Nullable final Direction side) {
        final Supplier<Capability<?>> capability = CAPABILITIES.get(type);
        if (capability == null) {
            LOGGER.warn("Trying to access type with no known capability: [{}]", type);
            return Optional.empty();
        }

        final LazyOptional<T> value = tileEntity.getCapability(capability.get(), side).cast();

        //noinspection ConstantConditions Can be null because we pass in null.
        return Optional.ofNullable(value.orElse(null));
    }
}
