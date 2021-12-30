package li.cil.oc2.common.util;

import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import java.util.function.Supplier;

public final class LocationSupplierUtils {
    public static Supplier<Optional<Location>> of(final BlockEntity tileEntity) {
        return () -> Location.of(tileEntity);
    }

    public static Supplier<Optional<Location>> of(final Entity entity) {
        return () -> Location.of(entity);
    }

    public static Supplier<Optional<Location>> of(final BlockDeviceQuery query) {
        final Optional<Location> location = Optional.of(new Location(query.getLevel(), query.getQueryPosition()));
        return () -> location;
    }

    public static Supplier<Optional<Location>> of(final ItemDeviceQuery query) {
        final Optional<BlockEntity> tileEntity = query.getContainerTileEntity();
        if (tileEntity.isPresent()) {
            return () -> Location.of(tileEntity.get());
        }

        final Optional<Entity> entity = query.getContainerEntity();
        if (entity.isPresent()) {
            return () -> Location.of(entity.get());
        }

        return Optional::empty;
    }
}
