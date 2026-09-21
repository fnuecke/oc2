/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.doc;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOCallbacks;
import li.cil.oc2.api.bus.device.io.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record DeviceDocumentation(@Nullable RPC rpc, @Nullable IO io) {
    public record RPC(List<String> typeNames, Optional<String> description, List<RPCMethod> methods) {
    }

    public record RPCMethod(String name, Class<?> returnType, Optional<String> description,
                            Optional<String> returnValueDescription, List<RPCParameter> parameters) {
    }

    public record RPCParameter(Optional<String> name, Class<?> type, Optional<String> description, boolean optional) {
    }

    public record IO(String name, Optional<String> description, List<IOMethod> methods) {
    }

    public record IOMethod(int code, String name, Optional<String> description,
                           Optional<String> argumentsDescription, Optional<String> resultsDescription) {
    }

    // --------------------------------------------------------------------- //

    public static DeviceDocumentation of(final Class<?> type) {
        return new DeviceDocumentation(Callbacks.hasMethods(type) ? rpc(type) : null, IOCallbacks.hasMethods(type) ? io(type) : null);
    }

    // --------------------------------------------------------------------- //

    private static RPC rpc(final Class<?> type) {
        final RPCDeviceDescription annotation = find(type, RPCDeviceDescription.class);
        return new RPC(
            Callbacks.getTypeNames(type),
            annotation != null ? text(annotation.description()) : Optional.empty(),
            methods(type, Callback.class).map(DeviceDocumentation::rpcMethod).toList());
    }

    private static RPCMethod rpcMethod(final Method method) {
        final Callback annotation = method.getAnnotation(Callback.class);
        return new RPCMethod(
            text(annotation.name()).orElse(method.getName()),
            method.getReturnType(),
            text(annotation.description()),
            text(annotation.returnValueDescription()),
            Arrays.stream(method.getParameters()).map(DeviceDocumentation::rpcParameter).toList());
    }

    private static RPCParameter rpcParameter(final java.lang.reflect.Parameter parameter) {
        final Parameter annotation = parameter.getAnnotation(Parameter.class);
        final Optional<String> name = annotation != null ? text(annotation.value()) : Optional.empty();
        return new RPCParameter(
            name.or(() -> parameter.isNamePresent() ? Optional.of(parameter.getName()) : Optional.empty()),
            parameter.getType(),
            annotation != null ? text(annotation.description()) : Optional.empty(),
            annotation != null && annotation.optional());
    }

    private static IO io(final Class<?> type) {
        final IODeviceDescription annotation = find(type, IODeviceDescription.class);
        return new IO(
            IOCallbacks.getName(type),
            annotation != null ? text(annotation.description()) : Optional.empty(),
            methods(type, IOCallback.class).map(DeviceDocumentation::ioMethod).toList());
    }

    private static IOMethod ioMethod(final Method method) {
        final IOCallback annotation = method.getAnnotation(IOCallback.class);
        return new IOMethod(
            annotation.value(),
            text(annotation.name()).orElse(method.getName()),
            text(annotation.description()),
            text(annotation.argumentsDescription()),
            text(annotation.resultsDescription()));
    }

    private static java.util.stream.Stream<Method> methods(final Class<?> type, final Class<? extends Annotation> annotation) {
        return Arrays.stream(type.getMethods()).filter(method -> method.isAnnotationPresent(annotation));
    }

    @Nullable
    private static <T extends Annotation> T find(final Class<?> type, final Class<T> annotation) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            final T found = current.getAnnotation(annotation);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Optional<String> text(final String value) {
        return Strings.isNotBlank(value) ? Optional.of(value) : Optional.empty();
    }
}
