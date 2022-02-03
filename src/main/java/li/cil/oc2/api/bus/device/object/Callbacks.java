/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.object;

import li.cil.oc2.api.bus.device.rpc.AbstractRPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
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

import static java.util.Objects.requireNonNull;

/**
 * Provides automated extraction of {@link RPCMethod}s from instances of
 * class with methods annotated with the {@link Callback} annotation.
 * <p>
 * Prefer using {@link ObjectDevice} instead of using this class directly.
 */
public final class Callbacks {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final Map<Class<?>, List<Method>> METHOD_BY_TYPE = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Method, RPCParameter[]> PARAMETERS_BY_METHOD = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Method, CallbackDocumentation> DOCUMENTATION_BY_METHOD = Collections.synchronizedMap(new HashMap<>());

    ///////////////////////////////////////////////////////////////////

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
    public static List<RPCMethodGroup> collectMethods(final Object methodContainer) {
        final List<Method> reflectedMethods = getMethods(methodContainer.getClass());

        final ArrayList<RPCMethodGroup> methods = new ArrayList<>();
        for (final Method method : reflectedMethods) {
            try {
                methods.add(new ObjectRPCMethod(methodContainer, method));
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
        if (object instanceof final Class<?> clazz) {
            return !getMethods(clazz).isEmpty();
        } else {
            return !getMethods(object.getClass()).isEmpty();
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static List<Method> getMethods(final Class<?> type) {
        synchronized (METHOD_BY_TYPE) {
            return METHOD_BY_TYPE.computeIfAbsent(type, c -> Arrays.stream(c.getMethods())
                .filter(m -> m.isAnnotationPresent(Callback.class))
                .collect(Collectors.toList()));
        }
    }

    private static final class ObjectRPCMethod extends AbstractRPCMethod {
        private final MethodHandle handle;
        private final String description;
        private final String returnValueDescription;

        public ObjectRPCMethod(final Object target, final Method method) throws IllegalAccessException {
            this(new ConstructorData(target, method));
        }

        private ObjectRPCMethod(final ConstructorData data) throws IllegalAccessException {
            super(data.methodName, data.annotation.synchronize(), data.method.getReturnType(), data.parameters);

            this.handle = MethodHandles.lookup().unreflect(data.method).bindTo(data.target);
            this.description = data.description;
            this.returnValueDescription = data.returnValueDescription;
        }

        @Nullable
        @Override
        protected Object invoke(final Object... parameters) throws Throwable {
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
            final ObjectRPCMethod that = (ObjectRPCMethod) o;
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

        // Utility class to precompute stuff for constructor before calling super constructor.
        private static final class ConstructorData {
            public final Object target;
            public final Method method;
            public final Callback annotation;
            public final String methodName;
            public final String description;
            public final String returnValueDescription;
            public final RPCParameter[] parameters;

            public ConstructorData(final Object target, final Method method) {
                this.target = target;
                this.method = method;
                this.annotation = requireNonNull(method.getAnnotation(Callback.class), "Method without Callback annotation.");
                this.methodName = Strings.isNotBlank(annotation.name()) ? annotation.name() : method.getName();

                final CallbackDocumentation documentation = DOCUMENTATION_BY_METHOD.computeIfAbsent(method, m -> {
                    final boolean hasDescription = Strings.isNotBlank(annotation.description());
                    final boolean hasReturnValueDescription = Strings.isNotBlank(annotation.returnValueDescription());

                    String description = hasDescription ? annotation.description() : null;
                    String returnValueDescription = hasReturnValueDescription ? annotation.returnValueDescription() : null;
                    final HashMap<String, String> parameterDescriptions = new HashMap<>();

                    if (target instanceof final DocumentedDevice documentedDevice) {
                        final DeviceVisitorImpl visitor = new DeviceVisitorImpl();
                        documentedDevice.getDeviceDocumentation(visitor);

                        final CallbackVisitorImpl callbackVisitor = visitor.callbacks.get(methodName);
                        if (callbackVisitor != null) {
                            if (Strings.isNotBlank(callbackVisitor.description)) {
                                description = callbackVisitor.description;
                            }
                            if (Strings.isNotBlank(callbackVisitor.returnValueDescription)) {
                                returnValueDescription = callbackVisitor.description;
                            }

                            parameterDescriptions.putAll(callbackVisitor.parameterDescriptions);
                        }
                    }

                    return new CallbackDocumentation(description, returnValueDescription, parameterDescriptions);
                });

                this.description = documentation.description;
                this.returnValueDescription = documentation.returnValueDescription;

                this.parameters = PARAMETERS_BY_METHOD.computeIfAbsent(method,
                    m -> Arrays.stream(m.getParameters())
                        .map(parameter -> new ReflectionParameter(parameter, documentation.parameterDescriptions))
                        .toArray(RPCParameter[]::new));
            }
        }

        private static final class ReflectionParameter implements RPCParameter {
            private final Class<?> type;
            @Nullable private final String name;
            @Nullable private final String description;

            public ReflectionParameter(final java.lang.reflect.Parameter parameter, final HashMap<String, String> parameterDescriptions) {
                this.type = parameter.getType();

                final Parameter annotation = parameter.getAnnotation(Parameter.class);
                final boolean hasName = annotation != null && Strings.isNotBlank(annotation.value());
                final boolean hasDescription = annotation != null && Strings.isNotBlank(annotation.description());

                this.name = hasName ? annotation.value() : (parameter.isNamePresent() ? parameter.getName() : null);

                if (parameterDescriptions.containsKey(this.name)) {
                    this.description = parameterDescriptions.get(this.name);
                } else if (hasDescription) {
                    this.description = annotation.description();
                } else {
                    this.description = null;
                }
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

    private record CallbackDocumentation(@Nullable String description,
                                         @Nullable String returnValueDescription,
                                         HashMap<String, String> parameterDescriptions) { }

    private static final class DeviceVisitorImpl implements DocumentedDevice.DeviceVisitor {
        public final HashMap<String, CallbackVisitorImpl> callbacks = new HashMap<>();

        @Override
        public DocumentedDevice.CallbackVisitor visitCallback(final String callbackName) {
            return callbacks.computeIfAbsent(callbackName, unused -> new CallbackVisitorImpl());
        }
    }

    private static final class CallbackVisitorImpl implements DocumentedDevice.CallbackVisitor {
        public String description;
        public String returnValueDescription;
        public final HashMap<String, String> parameterDescriptions = new HashMap<>();

        @Override
        public DocumentedDevice.CallbackVisitor description(final String value) {
            this.description = value;
            return this;
        }

        @Override
        public DocumentedDevice.CallbackVisitor returnValueDescription(final String value) {
            this.returnValueDescription = value;
            return this;
        }

        @Override
        public DocumentedDevice.CallbackVisitor parameterDescription(final String parameterName, final String value) {
            parameterDescriptions.put(parameterName, value);
            return this;
        }
    }
}
