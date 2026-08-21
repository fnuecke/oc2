/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public abstract class ModBlockEntity extends BlockEntity {
    private final Runnable onWorldUnloaded = this::onWorldUnloaded;
    private boolean needsWorldUnloadEvent;
    private boolean isUnloaded;

    // --------------------------------------------------------------------- //

    protected ModBlockEntity(final BlockEntityType<?> blockEntityType, final BlockPos pos, final BlockState state) {
        super(blockEntityType, pos, state);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (!isValid()) {
            return null;
        }

        final SingleResultCollector<T> collector = new SingleResultCollector<>(capability);
        collectCapabilities(collector, side);

        return collector.result;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();

        if (level == null) {
            return;
        }

        if (level.isClientSide()) {
            loadClient();
        } else {
            loadServer();

            ServerScheduler.schedule(level, () -> {
                if (isValid()) {
                    loadServerInLoadedLevel();
                }
            });

            if (needsWorldUnloadEvent) {
                ServerScheduler.scheduleOnUnload(level, onWorldUnloaded);
            }
        }
    }

    public void onChunkUnloaded() {
        Capabilities.invalidate(this);
        onUnload(false);
        isUnloaded = true;
    }

    public void onWorldUnloaded() {
        Capabilities.invalidate(this);
        onUnload(false);
        isUnloaded = true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Capabilities.invalidate(this);
        if (!isUnloaded) {
            onUnload(true);
        }
    }

    public boolean isValid() {
        return !isRemoved() && !isUnloaded;
    }

    @Nullable
    public AABB getExpandedRenderBoundingBox() {
        return null;
    }

    // --------------------------------------------------------------------- //

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

    protected void loadServerInLoadedLevel() {
    }

    protected void unloadServer(final boolean isRemove) {
    }

    // --------------------------------------------------------------------- //

    @FunctionalInterface
    protected interface CapabilityCollector {
        <T> void offer(CapabilityType<T> capability, T instance);
    }

    private static final class SingleResultCollector<T> implements CapabilityCollector {
        private final CapabilityType<T> capability;
        @Nullable
        private T result;

        private SingleResultCollector(final CapabilityType<T> capability) {
            this.capability = capability;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TOffered> void offer(final CapabilityType<TOffered> offeredCapability, final TOffered instance) {
            if (result == null && offeredCapability == capability) {
                result = (T) instance;
            }
        }
    }
}
