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
 * Prefer using {@link ObjectDevice} instead of using this class directly.
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
        final List<Method> reflectedMethods = METHOD_BY_TYPE.computeIfAbsent(methodContainer.getClass(), c -> Arrays.stream(c.getMethods())
                .filter(m -> m.isAnnotationPresent(Callback.class))
                .collect(Collectors.toList()));

        final List<DeviceMethod> methods = new ArrayList<>();
        for (final Method method : reflectedMethods) {
            try {
                methods.add(new ObjectDeviceMethod(methodContainer, method));
            } catch (final IllegalAccessException e) {
                LOGGER.error("Failed accessing method [{}].", method);
            }
        }

        return methods;
    }
}
