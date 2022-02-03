/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import li.cil.oc2.common.bus.RPCDeviceBusAdapter;

import java.lang.reflect.Type;
import java.util.UUID;

public final class MethodInvocationJsonDeserializer implements JsonDeserializer<RPCDeviceBusAdapter.MethodInvocation> {
    @Override
    public RPCDeviceBusAdapter.MethodInvocation deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        final UUID deviceId = context.deserialize(jsonObject.get("deviceId"), UUID.class);
        final String methodName = jsonObject.get("name").getAsString();
        final JsonElement parameters = jsonObject.get("parameters");
        return new RPCDeviceBusAdapter.MethodInvocation(deviceId, methodName, parameters != null && parameters.isJsonArray() ? parameters.getAsJsonArray() : new JsonArray());
    }
}
