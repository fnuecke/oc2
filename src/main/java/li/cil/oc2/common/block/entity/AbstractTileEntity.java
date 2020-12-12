package li.cil.oc2.common.block.entity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;

public abstract class AbstractTileEntity extends TileEntity {
    protected final HashMap<Capability<?>, LazyOptional<?>> capabilities = new HashMap<>();

    ///////////////////////////////////////////////////////////////////

    protected AbstractTileEntity(final TileEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull final Capability<T> capability, @Nullable final Direction side) {
        final LazyOptional<?> optional = capabilities.get(capability);
        if (optional != null) {
            return optional.cast();
        } else {
            return super.getCapability(capability, side);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        initialize();
    }

    @Override
    public void remove() {
        super.remove();
        dispose();
    }

    @Override
    public void onChunkUnloaded() {
        invalidateCaps();
        dispose();
    }

    ///////////////////////////////////////////////////////////////////

    protected <T> void setCapabilityIfAbsent(final Capability<T> capability, final T value) {
        capabilities.putIfAbsent(capability, LazyOptional.of(() -> value));
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        for (final LazyOptional<?> capability : capabilities.values()) {
            capability.invalidate();
        }
    }

    protected void initialize() {
        final World world = getWorld();
        if (world == null) {
            return;
        }

        if (world.isRemote()) {
            initializeClient();
        } else {
            initializeServer();
        }
    }

    protected void dispose() {
        final World world = getWorld();
        if (world == null) {
            return;
        }
        if (world.isRemote()) {
            disposeClient();
        } else {
            disposeServer();
        }
    }

    protected void initializeClient() {
    }

    protected void initializeServer() {
    }

    protected void disposeClient() {
    }

    protected void disposeServer() {
    }
}
