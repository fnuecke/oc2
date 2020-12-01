package li.cil.oc2.api.device.object;

import li.cil.oc2.api.device.DeviceMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides automated extraction of {@link DeviceMethod}s from instances of
 * class with methods annotated with the {@link Callback} annotation.
 * <p>
 * Prefer using {@link ObjectDeviceInterface} instead of using this class directly.
 */
public final class Callbacks {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashMap<Class<?>, List<Method>> METHOD_BY_TYPE = new HashMap<>();

    /**
     * Collects all methods annotated with {@link Callback} in the specified object
     * and generated {@link DeviceMethod}s for each one.
     * <p>
     * The generated {@link DeviceMethod} will be bound to the passed object and
     * can be called without needing to pass the object.
     * <p>
     * For example:
     * <pre>
     * class Example {
     *   &#64;Callback
     *   public void f(String a) { }
     * }
     *
     * List<DeviceMethod> methods = Callbacks.collectMethods(new Example());
     * methods.get(0).invoke("argument");
     * </pre>
     *
     * @param methodContainer an instance of a class with annotated methods.
     * @return the list of methods extracted from the specified object.
     */
    public static List<DeviceMethod> collectMethods(final Object methodContainer) {
        final List<Method> reflectedMethods = getMethods(methodContainer.getClass());

        final ArrayList<DeviceMethod> methods = new ArrayList<>();
        for (final Method method : reflectedMethods) {
            try {
                methods.add(new ObjectDeviceMethod(methodContainer, method));
            } catch (final IllegalAccessException e) {
                LOGGER.error("Failed accessing method [{}].", method);
            }
        }

        return methods;
    }

    /**
     * Returns whether any {@link Callback} annotated methods are present on the specified
     * object without creating bound {@link DeviceMethod} instances.
     * <p>
     * The specified {@code object} can be an instance or a {@link Class}.
     *
     * @param object the object to check for methods on.
     * @return {@code true} if any methods were found on the object; {@code false} otherwise.
     */
    public static boolean hasMethods(final Object object) {
        if (object instanceof Class<?>) {
            return !getMethods((Class<?>) object).isEmpty();

        } else {
            return !getMethods(object.getClass()).isEmpty();
        }
    }

    private static List<Method> getMethods(final Class<?> type) {
        return METHOD_BY_TYPE.computeIfAbsent(type, c -> Arrays.stream(c.getMethods())
                .filter(m -> m.isAnnotationPresent(Callback.class))
                .collect(Collectors.toList()));
    }
}
