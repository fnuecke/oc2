package li.cil.oc2.common.serialization.serializers;

import com.google.gson.*;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;

import java.lang.reflect.Type;

public final class RPCMethodJsonSerializer implements JsonSerializer<RPCMethod> {
    @Override
    public JsonElement serialize(final RPCMethod method, final Type typeOfMethod, final JsonSerializationContext context) {
        if (method == null) {
            return JsonNull.INSTANCE;
        }

        final JsonObject methodJson = new JsonObject();
        methodJson.addProperty("name", method.getName());
        methodJson.addProperty("returnType", method.getReturnType().getSimpleName());

        method.getDescription().ifPresent(s -> methodJson.addProperty("description", s));
        method.getReturnValueDescription().ifPresent(s -> methodJson.addProperty("returnValueDescription", s));

        final JsonArray parametersJson = new JsonArray();
        methodJson.add("parameters", parametersJson);

        final RPCParameter[] parameters = method.getParameters();
        for (final RPCParameter parameter : parameters) {
            final JsonObject parameterJson = new JsonObject();

            parameter.getName().ifPresent(s -> parameterJson.addProperty("name", s));
            parameter.getDescription().ifPresent(s -> parameterJson.addProperty("description", s));

            final Class<?> type = parameter.getType();
            parameterJson.addProperty("type", type.getSimpleName());

            parametersJson.add(parameterJson);
        }

        return methodJson;
    }
}
