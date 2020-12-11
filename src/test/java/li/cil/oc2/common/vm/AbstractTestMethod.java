package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;

import javax.annotation.Nullable;
import java.util.Optional;

abstract class AbstractTestMethod implements RPCMethod {
    private final Class<?> returnType;
    private final RPCParameter[] parameters;

    protected AbstractTestMethod(final Class<?> returnType, final Class<?>... parameterTypes) {
        this.returnType = returnType;
        parameters = new RPCParameter[parameterTypes.length];
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
    public RPCParameter[] getParameters() {
        return parameters;
    }

    private static final class TestParameter implements RPCParameter {
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
