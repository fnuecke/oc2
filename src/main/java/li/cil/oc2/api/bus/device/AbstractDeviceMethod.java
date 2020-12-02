package li.cil.oc2.api.bus.device;

/**
 * Convenience base class for {@link DeviceMethod} implementations.
 */
public abstract class AbstractDeviceMethod implements DeviceMethod {
    protected final String name;
    protected final boolean synchronize;
    protected final Class<?> returnType;
    protected final DeviceMethodParameter[] parameters;

    protected AbstractDeviceMethod(final String name, final boolean synchronize, final Class<?> returnType, final DeviceMethodParameter... parameters) {
        this.name = name;
        this.synchronize = synchronize;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    protected AbstractDeviceMethod(final String name, final Class<?> returnType, final DeviceMethodParameter... parameters) {
        this(name, false, returnType, parameters);
    }

    protected AbstractDeviceMethod(final String name, final boolean synchronize, final DeviceMethodParameter... parameters) {
        this(name, synchronize, void.class, parameters);
    }

    protected AbstractDeviceMethod(final String name, final DeviceMethodParameter... parameters) {
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
    public DeviceMethodParameter[] getParameters() {
        return parameters;
    }
}
