package li.cil.oc2.api.bus.device.object;

import li.cil.oc2.api.bus.device.rpc.AbstractRPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides automated extraction of {@link RPCMethod}s from instances of
 * class with methods annotated with the {@link Callback} annotation.
 * <p>
 * Prefer using {@link ObjectDevice} instead of using this class directly.
 */
public final class Callbacks {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashMap<Class<?>, List<Method>> METHOD_BY_TYPE = new HashMap<>();

    /**
     * Collects all methods annotated with {@link Callback} in the specified object
     * and generated {@link RPCMethod}s for each one.
     * <p>
     * The generated {@link RPCMethod} will be bound to the passed object and
     * can be called without needing to pass the object.
     * <p>
     * For example:
     * <pre>
     * class Example {
     *   &#64;Callback
     *   public void f(String a) { }
     * }
     *
     * List&lt;RPCMethod&gt; methods = Callbacks.collectMethods(new Example());
     * methods.get(0).invoke("argument");
     * </pre>
     *
     * @param methodContainer an instance of a class with annotated methods.
     * @return the list of methods extracted from the specified object.
     */
    public static List<RPCMethod> collectMethods(final Object methodContainer) {
        final List<Method> reflectedMethods = getMethods(methodContainer.getClass());

        final ArrayList<RPCMethod> methods = new ArrayList<>();
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
     * object without creating bound {@link RPCMethod} instances.
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

    private static final class ObjectDeviceMethod extends AbstractRPCMethod {
        private final MethodHandle handle;
        private final String description;
        private final String returnValueDescription;

        public ObjectDeviceMethod(final Object target, final Method method) throws IllegalAccessException {
            super(method.getName(),
                    Objects.requireNonNull(method.getAnnotation(Callback.class), "Method without Callback annotation.").synchronize(),
                    method.getReturnType(),
                    getParameters(method));

            final Callback annotation = method.getAnnotation(Callback.class);
            this.handle = MethodHandles.lookup().unreflect(method).bindTo(target);
            this.description = Strings.isNotBlank(annotation.description()) ? annotation.description() : null;
            this.returnValueDescription = Strings.isNotBlank(annotation.returnValueDescription()) ? annotation.returnValueDescription() : null;
        }

        @Nullable
        @Override
        public Object invoke(final Object... parameters) throws Throwable {
            return handle.invokeWithArguments(parameters);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getReturnValueDescription() {
            return Optional.ofNullable(returnValueDescription);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ObjectDeviceMethod that = (ObjectDeviceMethod) o;
            return handle.equals(that.handle);
        }

        @Override
        public int hashCode() {
            return Objects.hash(handle);
        }

        @Override
        public String toString() {
            return handle.toString();
        }

        private static RPCParameter[] getParameters(final Method method) {
            return Arrays.stream(method.getParameters())
                    .map(ReflectionParameter::new)
                    .toArray(RPCParameter[]::new);
        }

        private static final class ReflectionParameter implements RPCParameter {
            private final Class<?> type;
            @Nullable private final String name;
            @Nullable private final String description;

            public ReflectionParameter(final java.lang.reflect.Parameter parameter) {
                this.type = parameter.getType();

                final Parameter annotation = parameter.getAnnotation(Parameter.class);
                final boolean hasName = annotation != null && Strings.isNotBlank(annotation.value());
                final boolean hasDescription = annotation != null && Strings.isNotBlank(annotation.description());

                this.name = hasName ? annotation.value() : null;
                this.description = hasDescription ? annotation.description() : null;
            }

            @Override
            public Class<?> getType() {
                return type;
            }

            @Override
            public Optional<String> getName() {
                return Optional.ofNullable(name);
            }

            @Override
            public Optional<String> getDescription() {
                return Optional.ofNullable(description);
            }
        }
    }
}
