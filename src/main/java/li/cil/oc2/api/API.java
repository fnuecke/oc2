package li.cil.oc2.api;

import com.google.gson.GsonBuilder;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.imc.DeviceMethodParameterTypeAdapter;
import li.cil.oc2.api.provider.DeviceProvider;

import java.lang.reflect.Type;

public final class API {
    public static final String MOD_ID = "oc2";

    /**
     * IMC message for registering a {@link DeviceProvider}.
     * <p>
     * Example:
     * <pre>
     * InterModComms.sendTo(API.MOD_ID, API.IMC_ADD_DEVICE_PROVIDER, () -&gt; new DeviceProvider() { ... });
     * </pre>
     */
    public static final String IMC_ADD_DEVICE_PROVIDER = "addDeviceProvider";

    /**
     * IMC message for registering Gson type adapters for method parameter serialization and
     * deserialization.
     * <p>
     * Must be called with a supplier that provides an instance of {@link DeviceMethodParameterTypeAdapter}.
     * <p>
     * It can be necessary to register additional serializers when implementing {@link RPCMethod}s
     * that use custom parameter types.
     *
     * @see GsonBuilder#registerTypeAdapter(Type, Object)
     * @see RPCMethod
     * @see Callback
     */
    public static final String IMC_ADD_DEVICE_METHOD_PARAMETER_TYPE_ADAPTER = "addDeviceMethodParameterTypeAdapter";

    private API() {
    }
}
