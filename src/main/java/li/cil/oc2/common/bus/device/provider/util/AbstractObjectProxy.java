package li.cil.oc2.common.bus.device.provider.util;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractObjectProxy<T> {
    protected final T value;

    ///////////////////////////////////////////////////////////////////

    public AbstractObjectProxy(final T value) {
        this.value = value;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractObjectProxy<?> that = (AbstractObjectProxy<?>) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
