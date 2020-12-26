package li.cil.oc2.common.block.entity;

import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public abstract class AbstractTileEntity extends TileEntity {
    private final Runnable onWorldUnloaded = this::onWorldUnloaded;
    private final HashMap<CapabilityCacheKey, LazyOptional<?>> capabilityCache = new HashMap<>();
    private boolean needsWorldUnloadEvent;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTileEntity(final TileEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    ///////////////////////////////////////////////////////////////////

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull final Capability<T> capability, @Nullable final Direction side) {
        if (isRemoved()) {
            return LazyOptional.empty();
        }

        final CapabilityCacheKey key = new CapabilityCacheKey(capability, side);
        final LazyOptional<?> value;
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
                value = LazyOptional.of(() -> instance);
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

        final World world = getWorld();
        if (world == null) {
            return;
        }

        if (world.isRemote()) {
            loadClient();
        } else {
            loadServer();

            if (needsWorldUnloadEvent) {
                ServerScheduler.scheduleOnUnload(world, onWorldUnloaded);
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
    public void remove() {
        super.remove(); // -> invalidateCaps()
        onUnload();
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
    protected void invalidateCaps() {
        super.invalidateCaps();

        // Copy values because invalidate callback will modify map (removes invalidated entry).
        for (final LazyOptional<?> capability : new ArrayList<>(capabilityCache.values())) {
            capability.invalidate();
        }
    }

    protected void onUnload() {
        final World world = getWorld();
        if (world != null && !world.isRemote()) {
            unloadServer();
            ServerScheduler.cancelOnUnload(world, onWorldUnloaded);
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
