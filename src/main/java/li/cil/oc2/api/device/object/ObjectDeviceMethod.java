package li.cil.oc2.api.device.object;

import li.cil.oc2.api.device.AbstractDeviceMethod;
import li.cil.oc2.api.device.DeviceMethodParameter;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * Not intended to be instantiated directly, see {@link Callbacks}.
 *
 * @see Callbacks
 */
public final class ObjectDeviceMethod extends AbstractDeviceMethod {
    private final MethodHandle handle;
    private final String description;
    private final String returnValueDescription;

    public ObjectDeviceMethod(final Object target, final Method method) throws IllegalAccessException {
        super(method.getName(), method.getReturnType(), getParameters(method));

        final Callback annotation = method.getAnnotation(Callback.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Method without Callback annotation.");
        }

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

    private static DeviceMethodParameter[] getParameters(final Method method) {
        return Arrays.stream(method.getParameters())
                .map(ReflectionParameter::new)
                .toArray(DeviceMethodParameter[]::new);
    }

    private static final class ReflectionParameter implements DeviceMethodParameter {
        private final Class<?> type;
        @Nullable private final String name;
        @Nullable private final String description;

        public ReflectionParameter(final java.lang.reflect.Parameter parameter) {
            this.type = parameter.getType();

            final li.cil.oc2.api.device.object.Parameter annotation = parameter.getAnnotation(li.cil.oc2.api.device.object.Parameter.class);
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
