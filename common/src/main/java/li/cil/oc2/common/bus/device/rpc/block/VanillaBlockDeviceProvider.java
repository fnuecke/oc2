/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class VanillaBlockDeviceProvider extends AbstractBlockDeviceProvider {
    private final BiPredicate<LevelAccessor, BlockPos> predicate;
    private final Function<BlockLocation, Object> factory;

    // --------------------------------------------------------------------- //

    public static VanillaBlockDeviceProvider of(final Block block, final Function<BlockLocation, Object> factory) {
        return new VanillaBlockDeviceProvider((level, pos) -> level.getBlockState(pos).is(block), factory);
    }

    public static VanillaBlockDeviceProvider of(final Class<? extends BlockEntity> type, final Function<BlockLocation, Object> factory) {
        return new VanillaBlockDeviceProvider((level, pos) -> type.isInstance(level.getBlockEntity(pos)), factory);
    }

    private VanillaBlockDeviceProvider(final BiPredicate<LevelAccessor, BlockPos> predicate, final Function<BlockLocation, Object> factory) {
        this.predicate = predicate;
        this.factory = factory;
    }

    // --------------------------------------------------------------------- //

    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        if (!predicate.test(query.getLevel(), query.getQueryPosition())) {
            return Invalidatable.empty();
        }

        final BlockLocation location = new BlockLocation(new WeakReference<>(query.getLevel()), query.getQueryPosition());
        return Invalidatable.of(new ObjectDevice(factory.apply(location)));
    }
}
