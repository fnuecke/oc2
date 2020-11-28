package li.cil.oc2.api.device;

/**
 * Convenience base class for {@link DeviceMethod} implementations.
 */
public abstract class AbstractDeviceMethod implements DeviceMethod {
    protected final String name;
    protected final Class<?> returnType;
    protected final DeviceMethodParameter[] parameters;

    protected AbstractDeviceMethod(final String name, final Class<?> returnType, final DeviceMethodParameter... parameters) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    protected AbstractDeviceMethod(final String name, final DeviceMethodParameter... parameters) {
        this(name, void.class, parameters);
    }

    @Override
    public String getName() {
        return name;
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
