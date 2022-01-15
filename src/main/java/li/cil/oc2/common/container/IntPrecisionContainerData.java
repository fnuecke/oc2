package li.cil.oc2.common.container;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * Utility class for synchronizing full-precision ints using {@link ContainerData}.
 */
public abstract class IntPrecisionContainerData implements ContainerData {
    public abstract int getInt(final int index);

    public abstract int getIntCount();

    public static abstract class Server extends IntPrecisionContainerData {
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
            throw new UnsupportedOperationException();
        }

        @Override
        public int getCount() {
            return getIntCount() * 2;
        }
    }

    public static final class Client extends IntPrecisionContainerData {
        private final SimpleContainerData data;

        public Client(final int size) {
            data = new SimpleContainerData(size * 2);
        }

        @Override
        public int getInt(final int index) {
            return (get(index * 2 + 1) << 16) | get(index * 2);
        }

        @Override
        public int getIntCount() {
            return getCount() / 2;
        }

        @Override
        public int get(final int index) {
            return data.get(index);
        }

        @Override
        public void set(final int index, final int value) {
            data.set(index, value & 0xFFFF);
        }

        @Override
        public int getCount() {
            return data.getCount();
        }
    }
}
