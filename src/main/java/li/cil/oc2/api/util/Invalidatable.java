/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.util;

import li.cil.oc2.common.util.RunnableUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Wrapper for objects which may become invalid, such as {@link li.cil.oc2.api.bus.device.Device}s.
 * <p>
 * This implementation allows listeners added via {@link #addListener(Consumer)} to be removed again
 * using the returned token. This allows avoiding memory leaks due to inversion of reference ownership,
 * where an observable keeps observers alive due to its listener list.
 *
 * @param <T> The type of the underlying value.
 */
public final class Invalidatable<T> {
    @FunctionalInterface
    public interface ListenerToken {
        void removeListener();
    }

    @SuppressWarnings("rawtypes")
    private static final Invalidatable EMPTY = new Invalidatable();

    @SuppressWarnings("unchecked")
    public static <T> Invalidatable<T> empty() {
        return EMPTY;
    }

    public static <T> Invalidatable<T> of(final T value) {
        return new Invalidatable<>(value);
    }

    private final List<Consumer<Invalidatable<T>>> listeners = new ArrayList<>();
    private T value;
    private boolean isValid = true;

    public Invalidatable(final T value) {
        this.value = value;
    }

    private Invalidatable() {
        this.value = null;
        this.isValid = false;
    }

    public T get() {
        if (isValid) {
            assert value != null;
            return value;
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean isPresent() {
        return isValid;
    }

    public void ifPresent(final Consumer<T> consumer) {
        if (isValid) {
            consumer.accept(value);
        }
    }

    public <U> Invalidatable<U> mapWithDependency(final Function<T, U> mapper) {
        if (!isValid) {
            return empty();
        }

        // Map to new type.
        final Invalidatable<U> mapped = new Invalidatable<>(mapper.apply(value));

        // When this instance gets invalidated, invalidate mapped value.
        final ListenerToken token = this.addListener(unused -> mapped.invalidate());

        // When mapped value gets invalidated, remove listener.
        mapped.addListener(unused -> token.removeListener());

        return mapped;
    }

    public void invalidate() {
        if (isValid) {
            isValid = false;
            value = null;
            listeners.forEach(listener -> listener.accept(this));
            listeners.clear();
        }
    }

    public ListenerToken addListener(final Consumer<Invalidatable<T>> listener) {
        if (isValid) {
            listeners.add(listener);
            return () -> {
                if (isValid) {
                    listeners.remove(listener);
                }
            };
        } else {
            listener.accept(this);
            return RunnableUtils::doNothing;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Invalidatable<?> that = (Invalidatable<?>) o;
        return isValid == that.isValid && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, isValid);
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "Invalidated";
    }
}
