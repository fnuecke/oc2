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

import javax.annotation.Nullable;
import java.util.ArrayList;

public abstract class ModBlockEntity extends BlockEntity {
    private final Runnable onWorldUnloaded = this::onWorldUnloaded;
    private boolean needsWorldUnloadEvent;
    private boolean isUnloaded;

    ///////////////////////////////////////////////////////////////////

    protected ModBlockEntity(final BlockEntityType<?> blockEntityType, final BlockPos pos, final BlockState state) {
        super(blockEntityType, pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (!isValid()) {
            return null;
        }

        final ArrayList<T> list = new ArrayList<>();
        collectCapabilities(new CapabilityCollector() {
            @SuppressWarnings("unchecked")
            @Override
            public <TOffered> void offer(final CapabilityType<TOffered> offeredCapability, final TOffered instance) {
                if (offeredCapability == capability) {
                    list.add((T) instance);
                }
            }
        }, side);

        return list.isEmpty() ? null : list.get(0);
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

    ///////////////////////////////////////////////////////////////////

    protected void invalidateCapabilities() {
        Capabilities.invalidate(this);
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
        <T> void offer(CapabilityType<T> capability, T instance);
    }
}
