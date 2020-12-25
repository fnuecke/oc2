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
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public abstract class AbstractTileEntity extends TileEntity {
    protected final Set<LazyOptional<?>> capabilities = Collections.newSetFromMap(new WeakHashMap<>());
    protected boolean needsWorldUnloadEvent;

    private final Runnable onWorldUnloaded = this::onWorldUnloaded;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTileEntity(final TileEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    ///////////////////////////////////////////////////////////////////

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull final Capability<T> capability, @Nullable final Direction side) {
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
            final LazyOptional<T> optional = LazyOptional.of(() -> list.get(0));
            capabilities.add(optional);
            return optional;
        }

        return super.getCapability(capability, side);
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

    @FunctionalInterface
    protected interface CapabilityCollector {
        <T> void offer(Capability<T> capability, T instance);
    }

    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        for (final LazyOptional<?> capability : capabilities) {
            capability.invalidate();
        }
    }

    protected void onUnload() {
        final World world = getWorld();
        if (world == null) {
            return;
        }
        if (!world.isRemote()) {
            unloadServer();
            ServerScheduler.cancelOnUnload(world, onWorldUnloaded);
        }
    }

    protected void loadClient() {
    }

    protected void loadServer() {
    }

    protected void unloadServer() {
    }
}
