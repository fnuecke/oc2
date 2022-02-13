/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.util.LazyOptionalUtils;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class ModBlockEntity extends BlockEntity {
    private final Runnable onWorldUnloaded = this::onWorldUnloaded;
    private final HashMap<CapabilityCacheKey, LazyOptional<?>> capabilityCache = new HashMap<>();
    private boolean needsWorldUnloadEvent;
    private boolean isUnloaded;

    ///////////////////////////////////////////////////////////////////

    protected ModBlockEntity(final BlockEntityType<?> blockEntityType, final BlockPos pos, final BlockState state) {
        super(blockEntityType, pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction side) {
        if (!isValid()) {
            return LazyOptional.empty();
        }

        final CapabilityCacheKey key = new CapabilityCacheKey(capability, side);
        LazyOptional<?> value;
        if (capabilityCache.containsKey(key)) {
            value = capabilityCache.get(key);
        } else {
            value = LazyOptional.empty();
        }

        if (!value.isPresent()) {
            final ArrayList<T> list = new ArrayList<>();
            collectCapabilities(new CapabilityCollector() {
                @SuppressWarnings("unchecked")
                @Override
                public <TOffered> void offer(final Capability<TOffered> offeredCapability, final TOffered instance) {
                    if (offeredCapability == capability) {
                        list.add((T) instance);
                    }
                }
            }, side);

            if (!list.isEmpty()) {
                final T instance = list.get(0);
                value = LazyOptional.of(() -> instance);
            } else {
                value = super.getCapability(capability, side);
            }

            if (value.isPresent()) {
                capabilityCache.put(key, value);
                LazyOptionalUtils.addWeakListener(value, capabilityCache, (map, optional) -> map.remove(key, optional));
            }
        }

        return value.cast();
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level == null) {
            return;
        }

        if (level.isClientSide()) {
            loadClient();
        } else {
            loadServer();

            if (needsWorldUnloadEvent) {
                ServerScheduler.scheduleOnUnload(level, onWorldUnloaded);
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded(); // -> invalidateCaps()
        onUnload(false);
        isUnloaded = true;
    }

    public void onWorldUnloaded() {
        invalidateCaps();
        onUnload(false);
    }

    @Override
    public void setRemoved() {
        super.setRemoved(); // -> invalidateCaps()
        if (!isUnloaded) {
            onUnload(true);
        }
    }

    public boolean isValid() {
        return !isRemoved() && !isUnloaded;
    }

    ///////////////////////////////////////////////////////////////////

    protected <T> void invalidateCapability(final Capability<T> capability, @Nullable final Direction direction) {
        final CapabilityCacheKey key = new CapabilityCacheKey(capability, direction);
        final LazyOptional<?> value = capabilityCache.get(key);
        if (value != null) {
            value.invalidate();
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();

        // Copy values because invalidate callback will modify map (removes invalidated entry).
        for (final LazyOptional<?> capability : new ArrayList<>(capabilityCache.values())) {
            capability.invalidate();
        }
    }

    protected void onUnload(final boolean isRemove) {
        if (level != null && !level.isClientSide()) {
            unloadServer(isRemove);
            ServerScheduler.cancelOnUnload(level, onWorldUnloaded);
        }
    }

    protected void setNeedsLevelUnloadEvent() {
        needsWorldUnloadEvent = true;
    }

    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
    }

    protected void loadClient() {
    }

    protected void loadServer() {
    }

    protected void unloadServer(final boolean isRemove) {
    }

    ///////////////////////////////////////////////////////////////////

    @FunctionalInterface
    protected interface CapabilityCollector {
        <T> void offer(Capability<T> capability, T instance);
    }

    ///////////////////////////////////////////////////////////////////

    private record CapabilityCacheKey(Capability<?> capability, @Nullable Direction direction) { }
}
