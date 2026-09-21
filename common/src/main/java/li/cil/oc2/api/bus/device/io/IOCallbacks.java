/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides automated extraction of {@link IOMethod}s from instances of
 * classes with methods annotated with the {@link IOCallback} annotation.
 * <p>
 * Prefer using {@link li.cil.oc2.api.bus.device.object.ObjectDevice} instead of using this class
 * directly.
 *
 * @see IOMethod
 * @see IOCallback
 * @see li.cil.oc2.api.bus.device.object.Callbacks
 */
public final class IOCallbacks {
    private static final Map<Class<?>, List<Method>> METHOD_BY_TYPE = Collections.synchronizedMap(new HashMap<>());

    // --------------------------------------------------------------------- //

    /**
     * Collects all methods annotated with {@link IOCallback} in the specified object
     * and generated {@link IOMethod}s for each one.
     * <p>
     * The generated {@link IOMethod} will be bound to the passed object and
     * can be called without needing to pass the object.
     *
     * @param methodContainer an instance of a class with annotated methods.
     * @return the list of methods extracted from the specified object.
     * @throws IllegalArgumentException if an annotated method has an unsupported signature or an
     *                                  invalid method code, or if two share a method code.
     */
    public static List<IOMethod> collectMethods(final Object methodContainer) {
        final List<Method> reflectedMethods = getMethods(methodContainer.getClass());

        final ArrayList<IOMethod> methods = new ArrayList<>(reflectedMethods.size());
        final Set<Integer> codes = new HashSet<>();
        for (final Method method : reflectedMethods) {
            final IOCallback annotation = method.getAnnotation(IOCallback.class);
            final int code = annotation.value();
            if (code < 0 || code > IOCallback.MAX_CODE) {
                throw new IllegalArgumentException("Function code [" + code + "] on [" + method +
                    "] is outside [0, " + IOCallback.MAX_CODE + "].");
            }
            if (!codes.add(code)) {
                throw new IllegalArgumentException("Duplicate method code [" + code + "] on [" + method + "].");
            }

            methods.add(new ObjectIOMethod(methodContainer, method, code, annotation.synchronize()));
        }

        return methods;
    }

    /**
     * Returns whether any {@link IOCallback} annotated methods are present on the specified
     * object without creating bound {@link IOMethod} instances.
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

    /**
     * Returns the guest-visible name declared by the specified object's {@link IODeviceDescription}.
     * <p>
     * The specified {@code object} can be an instance or a {@link Class}.
     *
     * @param object the object to read the name from.
     * @return the declared name.
     * @throws IllegalArgumentException if the annotation is absent or its name is not valid.
     */
    public static String getName(final Object object) {
        final Class<?> type = object instanceof final Class<?> clazz ? clazz : object.getClass();
        final String name = requireDescription(type).name();
        if (name.isEmpty() || name.length() > IODeviceDescription.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Device name [" + name + "] on [" + type.getName() +
                "] is not between 1 and " + IODeviceDescription.MAX_NAME_LENGTH + " characters.");
        }
        for (int i = 0; i < name.length(); i++) {
            final char character = name.charAt(i);
            if (character < 0x20 || character > 0x7E) {
                throw new IllegalArgumentException("Device name [" + name + "] on [" + type.getName() +
                    "] is not printable ASCII.");
            }
        }

        return name;
    }

    // --------------------------------------------------------------------- //

    private static List<Method> getMethods(final Class<?> type) {
        synchronized (METHOD_BY_TYPE) {
            return METHOD_BY_TYPE.computeIfAbsent(type, c -> Arrays.stream(c.getMethods())
                .filter(m -> m.isAnnotationPresent(IOCallback.class))
                .collect(Collectors.toList()));
        }
    }

    private static IODeviceDescription requireDescription(final Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            final IODeviceDescription annotation = current.getAnnotation(IODeviceDescription.class);
            if (annotation != null) {
                return annotation;
            }
        }
        throw new IllegalArgumentException("Missing " + IODeviceDescription.class.getSimpleName() +
            " on [" + type.getName() + "], which provides " + IOCallback.class.getSimpleName() + " methods.");
    }

    private IOCallbacks() {
    }

    // --------------------------------------------------------------------- //

    private enum Signature {
        NONE,
        ARGUMENTS,
        RESULTS,
        ARGUMENTS_AND_RESULTS;

        static Signature of(final Method method) {
            if (method.getReturnType() != void.class) {
                throw new IllegalArgumentException("Method [" + method + "] must return void.");
            }

            final Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 0) {
                return NONE;
            }
            if (parameters.length == 1 && isValidInArgument(parameters[0])) {
                return ARGUMENTS;
            }
            if (parameters.length == 1 && isValidOutArgument(parameters[0])) {
                return RESULTS;
            }
            if (parameters.length == 2 && isValidInArgument(parameters[0]) && isValidOutArgument(parameters[1])) {
                return ARGUMENTS_AND_RESULTS;
            }

            throw new IllegalArgumentException("Method [" + method + "] must take no parameters, " +
                "arguments, results, or arguments and results. Arguments are an " +
                IOInputStream.class.getSimpleName() + " or a plain " + InputStream.class.getSimpleName() +
                "; results an " + IOOutputStream.class.getSimpleName() + " or a plain " +
                OutputStream.class.getSimpleName() + ".");
        }

        private static boolean isValidInArgument(final Class<?> type) {
            return type == IOInputStream.class || type == InputStream.class;
        }

        private static boolean isValidOutArgument(final Class<?> type) {
            return type == IOOutputStream.class || type == OutputStream.class;
        }
    }

    private static final class ObjectIOMethod implements IOMethod {
        private final MethodHandle handle;
        private final Signature signature;
        private final int code;
        private final boolean isSynchronized;

        ObjectIOMethod(final Object target, final Method method, final int code, final boolean isSynchronized) {
            this.signature = Signature.of(method);
            this.code = code;
            this.isSynchronized = isSynchronized;
            try {
                this.handle = MethodHandles.lookup().unreflect(method).bindTo(target);
            } catch (final IllegalAccessException e) {
                throw new IllegalArgumentException("Failed accessing method [" + method + "].", e);
            }
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public boolean isSynchronized() {
            return isSynchronized;
        }

        @Override
        public void invoke(final InputStream arguments, final OutputStream results) throws Throwable {
            switch (signature) {
                case NONE -> handle.invoke();
                case ARGUMENTS -> handle.invoke(wrap(arguments));
                case RESULTS -> handle.invoke(wrap(results));
                case ARGUMENTS_AND_RESULTS -> handle.invoke(wrap(arguments), wrap(results));
            }
        }

        private static IOInputStream wrap(final InputStream stream) {
            return stream instanceof final IOInputStream wrapped ? wrapped : new IOInputStream(stream);
        }

        private static IOOutputStream wrap(final OutputStream stream) {
            return stream instanceof final IOOutputStream wrapped ? wrapped : new IOOutputStream(stream);
        }

        @Override
        public String toString() {
            return handle.toString();
        }
    }
}
