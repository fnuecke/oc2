package li.cil.oc2.common.container;

import net.minecraft.world.inventory.ContainerData;

/**
 * Utility class for synchronizing full-precision ints using {@link ContainerData}.
 * <p>
 * Expects either {@link #getInt(int)} and {@link #getIntCount()} or {@link #get(int)} and {@link #getCount()} to be
 * overridden.
 */
public abstract class IntPrecisionContainerData implements ContainerData {
    public int getInt(final int index) {
        return (get(index * 2 + 1) << 16) | get(index * 2);
    }

    public void setInt(final int index, final int value) {
    }

    public int getIntCount() {
        return getCount() / 2;
    }

    @Override
    public int get(final int index) {
        final int intValue = getInt(index / 2);
        if ((index & 1) == 0) {
            return intValue & 0xFFFF; // Low half.
        } else {
            return (intValue >>> 16) & 0xFFFF; // High half.
        }
    }

    @Override
    public void set(final int index, final int value) {
    }

    @Override
    public int getCount() {
        return getIntCount() * 2;
    }
}
