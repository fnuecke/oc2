package li.cil.oc2.common.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;


public class NullStorage<T> implements Capability.IStorage<T> {
    @Nullable
    @Override
    public Tag writeNBT(final Capability<T> capability, final T instance, final Direction side) {
        return null;
    }

    @Override
    public void readNBT(final Capability<T> capability, final T instance, final Direction side, final Tag nbt) {
    }
}
