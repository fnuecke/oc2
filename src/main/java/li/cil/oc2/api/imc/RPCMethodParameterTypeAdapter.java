package li.cil.oc2.api.imc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;

import java.lang.reflect.Type;

/**
 * Defines a type adapter implementation that should be used when marshalling
 * parameters of an invocation on an {@link RPCDevice}.
 * <p>
 * Registered adapters will be directly applied to the {@link Gson} instance
 * used for serialization/deserialization of parameters, i.e. the specified
 * {@link #typeAdapter}s should be valid for passing to {@link GsonBuilder#registerTypeAdapter(Type, Object)}.
 */
public final class RPCMethodParameterTypeAdapter {
    private final Class<?> type;
    private final Object typeAdapter;

    ///////////////////////////////////////////////////////////////////

    /**
     * Creates a new definition of a type adapter that can be called in the
     * {@link API#IMC_ADD_RPC_METHOD_PARAMETER_TYPE_ADAPTER} IMC message.
     *
     * @param type        the type the adapter is registered for.
     * @param typeAdapter the adapter to use for the specified type.
     */
    public RPCMethodParameterTypeAdapter(final Class<?> type, final Object typeAdapter) {
        this.type = type;
        this.typeAdapter = typeAdapter;
    }

    /**
     * The type the adapter is to be registered for.
     *
     * @return the serialized type.
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * The type adapter to use for the type.
     *
     * @return the type adapter.
     */
    public Object getTypeAdapter() {
        return typeAdapter;
    }
}
