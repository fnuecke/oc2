package li.cil.oc2.vm;

import li.cil.oc2.api.device.DeviceMethod;
import li.cil.oc2.api.device.DeviceMethodParameter;

import javax.annotation.Nullable;
import java.util.Optional;

abstract class AbstractTestMethod implements DeviceMethod {
    private final Class<?> returnType;
    private final DeviceMethodParameter[] parameters;

    protected AbstractTestMethod(final Class<?> returnType, final Class<?>... parameterTypes) {
        this.returnType = returnType;
        parameters = new DeviceMethodParameter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = new TestParameter("arg" + i, parameterTypes[i]);
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean isSynchronized() {
        return false;
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public DeviceMethodParameter[] getParameters() {
        return parameters;
    }

    private static final class TestParameter implements DeviceMethodParameter {
        @Nullable private final String name;
        private final Class<?> type;

        public TestParameter(@Nullable final String name, final Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public Optional<String> getName() {
            return Optional.ofNullable(name);
        }

        @Override
        public Class<?> getType() {
            return type;
        }
    }
}
