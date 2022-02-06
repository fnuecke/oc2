/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Supplier;

public final class LocationSupplierUtils {
    public static Supplier<Optional<BlockLocation>> of(final BlockEntity blockEntity) {
        return () -> BlockLocation.of(blockEntity);
    }

    public static Supplier<Optional<BlockLocation>> of(final Entity entity) {
        return () -> BlockLocation.of(entity);
    }

    public static Supplier<Optional<BlockLocation>> of(final BlockDeviceQuery query) {
        final Optional<BlockLocation> location = Optional.of(new BlockLocation(new WeakReference<>(query.getLevel()), query.getQueryPosition()));
        return () -> location;
    }

    public static Supplier<Optional<BlockLocation>> of(final ItemDeviceQuery query) {
        final Optional<BlockEntity> blockEntity = query.getContainerBlockEntity();
        if (blockEntity.isPresent()) {
            return () -> BlockLocation.of(blockEntity.get());
        }

        final Optional<Entity> entity = query.getContainerEntity();
        if (entity.isPresent()) {
            return () -> BlockLocation.of(entity.get());
        }

        return Optional::empty;
    }
}
