package li.cil.oc2.common.util;

import net.minecraftforge.common.util.LazyOptional;

import java.util.Objects;

public final class LazyOptionalUtils {
    public static <T1, T2> boolean equals(final LazyOptional<T1> optionalA, final LazyOptional<T2> optionalB) {
        if (optionalA.isPresent() != optionalB.isPresent()) {
            return false;
        }

        if (!optionalA.isPresent()) {
            return true;
        }

        final T1 valueA = optionalA.orElseThrow(AssertionError::new);
        final T2 valueB = optionalB.orElseThrow(AssertionError::new);
        return Objects.equals(valueA, valueB);
    }

    public static <T> int hashCode(final LazyOptional<T> optional) {
        return optional.map(Objects::hash).orElse(0);
    }
}
