package li.cil.oc2.common.util;

import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import java.util.function.Supplier;

public final class LocationSupplierUtils {
    public static Supplier<Optional<Location>> of(final BlockEntity blockEntity) {
        return () -> Location.of(blockEntity);
    }

    public static Supplier<Optional<Location>> of(final Entity entity) {
        return () -> Location.of(entity);
    }

    public static Supplier<Optional<Location>> of(final BlockDeviceQuery query) {
        final Optional<Location> location = Optional.of(new Location(query.getLevel(), query.getQueryPosition()));
        return () -> location;
    }

    public static Supplier<Optional<Location>> of(final ItemDeviceQuery query) {
        final Optional<BlockEntity> blockEntity = query.getContainerBlockEntity();
        if (blockEntity.isPresent()) {
            return () -> Location.of(blockEntity.get());
        }

        final Optional<Entity> entity = query.getContainerEntity();
        if (entity.isPresent()) {
            return () -> Location.of(entity.get());
        }

        return Optional::empty;
    }
}
