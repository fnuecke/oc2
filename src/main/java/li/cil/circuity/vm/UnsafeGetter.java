package li.cil.circuity.vm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class UnsafeGetter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Unsafe UNSAFE;

    static {
        Unsafe instance = null;
        final Field field;
        try {
            field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            instance = (Unsafe) field.get(null);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error(e);
        }

        UNSAFE = instance;
    }

    public static Unsafe get() {
        return UNSAFE;
    }
}
