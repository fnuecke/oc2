package li.cil.oc2.common.util;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;

public final class LazyOptionalUtils {
    /**
     * Adds a <em>weak</em> listener to a {@link LazyOptional}.
     * <p>
     * {@link LazyOptional}s do not allow removing listeners again, so once added, they will prevent garbage collection
     * of listeners until they go out of scope themselves. This may result in undesired "memory leaks". To work around
     * this, we register a light-weight proxy listener instead, which only keeps a weak reference to the actual
     * listener implementation context.
     *
     * @param optional  the optional to add the listener to.
     * @param weakValue the value that should only be referenced weakly, allowing garbage collection.
     * @param listener  the listener proxy which will take the weak value as context, if it still exists.
     * @param <T>       the type of the optional.
     * @param <U>       the type of the listener context.
     */
    public static <T, U> void addWeakListener(final LazyOptional<T> optional, final U weakValue, final BiConsumer<U, LazyOptional<T>> listener) {
        optional.addListener(buildListener(new WeakReference<U>(weakValue), listener));
    }

    private static <T, U> NonNullConsumer<LazyOptional<T>> buildListener(final WeakReference<U> weakValue, final BiConsumer<U, LazyOptional<T>> listener) {
        return capability -> {
            final U value = weakValue.get();
            if (value != null) {
                listener.accept(value, capability);
            }
        };
    }
}
