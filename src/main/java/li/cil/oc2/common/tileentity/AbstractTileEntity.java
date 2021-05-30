package li.cil.oc2.common.tileentity;

import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractBlockEntity extends BlockEntity {
    private final Runnable onWorldUnloaded = this::onWorldUnloaded;
    private final HashMap<CapabilityCacheKey, Optional<?>> capabilityCache = new HashMap<>();
    private boolean needsWorldUnloadEvent;

    ///////////////////////////////////////////////////////////////////

    protected AbstractBlockEntity(final BlockEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public <T> Optional<T> getCapability(final Capability<T> capability, @Nullable final Direction side) {
        if (isRemoved()) {
            return Optional.empty();
        }

        final CapabilityCacheKey key = new CapabilityCacheKey(capability, side);
        final Optional<?> value;
        if (capabilityCache.containsKey(key)) {
            value = capabilityCache.get(key);
        } else {
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
                value = Optional.of(() -> instance);
            } else {
                value = super.getCapability(capability, side);
            }

            if (value.isPresent()) {
                capabilityCache.put(key, value);
                value.addListener(optional -> capabilityCache.remove(key, optional));
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

        if (level.isClientSide) {
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
        onUnload();
    }

    public void onWorldUnloaded() {
        invalidateCaps();
        onUnload();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        onUnload();
    }

    ///////////////////////////////////////////////////////////////////

    protected <T> void invalidateCapability(final Capability<T> capability, @Nullable final Direction direction) {
        final CapabilityCacheKey key = new CapabilityCacheKey(capability, direction);
        final Optional<?> value = capabilityCache.get(key);
        if (value != null) {
            value.invalidate();
        }
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();

        // Copy values because invalidate callback will modify map (removes invalidated entry).
        for (final Optional<?> capability : new ArrayList<>(capabilityCache.values())) {
            capability.invalidate();
        }
    }

    protected void onUnload() {
        if (level != null && !level.isClientSide) {
            unloadServer();
            ServerScheduler.cancelOnUnload(level, onWorldUnloaded);
        }
    }

    protected void setNeedsWorldUnloadEvent() {
        needsWorldUnloadEvent = true;
    }

    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
    }

    protected void loadClient() {
    }

    protected void loadServer() {
    }

    protected void unloadServer() {
    }

    ///////////////////////////////////////////////////////////////////

    @FunctionalInterface
    protected interface CapabilityCollector {
        <T> void offer(Capability<T> capability, T instance);
    }

    ///////////////////////////////////////////////////////////////////

    private static final class CapabilityCacheKey {
        public final Capability<?> capability;
        @Nullable public final Direction direction;

        public CapabilityCacheKey(final Capability<?> capability, @Nullable final Direction direction) {
            this.capability = capability;
            this.direction = direction;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final CapabilityCacheKey that = (CapabilityCacheKey) o;
            return capability.equals(that.capability) && direction == that.direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(capability, direction);
        }
    }
}
