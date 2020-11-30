package li.cil.oc2.common.tile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;

public abstract class AbstractTileEntity extends TileEntity {
    protected final HashMap<Capability<?>, LazyOptional<?>> capabilities = new HashMap<>();

    protected AbstractTileEntity(final TileEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    protected <T> void addCapability(final Capability<T> capability, final T value) {
        capabilities.put(capability, LazyOptional.of(() -> value));
    }

    protected void initialize() {
    }

    protected void dispose() {
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

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        for (final LazyOptional<?> capability : capabilities.values()) {
            capability.invalidate();
        }
    }
}
