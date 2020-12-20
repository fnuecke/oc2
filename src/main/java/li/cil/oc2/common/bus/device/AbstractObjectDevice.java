package li.cil.oc2.common.bus.device;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractObjectDevice<T> {
    protected final T value;

    ///////////////////////////////////////////////////////////////////

    public AbstractObjectDevice(final T value) {
        this.value = value;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractObjectDevice<?> that = (AbstractObjectDevice<?>) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
