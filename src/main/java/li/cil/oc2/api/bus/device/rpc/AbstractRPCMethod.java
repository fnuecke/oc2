package li.cil.oc2.api.bus.device.rpc;

/**
 * Convenience base class for {@link RPCMethod} implementations.
 */
public abstract class AbstractRPCMethod implements RPCMethod {
    protected final String name;
    protected final boolean synchronize;
    protected final Class<?> returnType;
    protected final RPCParameter[] parameters;

    ///////////////////////////////////////////////////////////////////

    protected AbstractRPCMethod(final String name, final boolean synchronize, final Class<?> returnType, final RPCParameter... parameters) {
        this.name = name;
        this.synchronize = synchronize;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    protected AbstractRPCMethod(final String name, final Class<?> returnType, final RPCParameter... parameters) {
        this(name, false, returnType, parameters);
    }

    protected AbstractRPCMethod(final String name, final boolean synchronize, final RPCParameter... parameters) {
        this(name, synchronize, void.class, parameters);
    }

    protected AbstractRPCMethod(final String name, final RPCParameter... parameters) {
        this(name, false, void.class, parameters);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isSynchronized() {
        return synchronize;
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public RPCParameter[] getParameters() {
        return parameters;
    }
}
