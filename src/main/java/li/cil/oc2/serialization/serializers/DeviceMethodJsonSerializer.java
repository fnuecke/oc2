package li.cil.oc2.serialization.serializers;

import com.google.gson.*;
import li.cil.oc2.api.device.DeviceMethod;
import li.cil.oc2.api.device.DeviceMethodParameter;

import java.lang.reflect.Type;

public final class DeviceMethodJsonSerializer implements JsonSerializer<DeviceMethod> {
    @Override
    public JsonElement serialize(final DeviceMethod method, final Type typeOfMethod, final JsonSerializationContext context) {
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

        final DeviceMethodParameter[] parameters = method.getParameters();
        for (final DeviceMethodParameter parameter : parameters) {
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
