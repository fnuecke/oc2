package li.cil.oc2.common.capabilities;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class NullStorage<T> implements Capability.IStorage<T> {
    @Nullable
    @Override
    public INBT writeNBT(final Capability<T> capability, final T instance, final Direction side) {
        return null;
    }

    @Override
    public void readNBT(final Capability<T> capability, final T instance, final Direction side, final INBT nbt) {
    }
}
