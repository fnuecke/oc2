/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import li.cil.oc2.common.bus.RPCDeviceBusAdapter;

import java.lang.reflect.Type;

public final class EmptyRPCMethodGroupSerializer implements JsonSerializer<RPCDeviceBusAdapter.EmptyMethodGroup> {
    @Override
    public JsonElement serialize(final RPCDeviceBusAdapter.EmptyMethodGroup methodGroup, final Type typeOfMethodGroup, final JsonSerializationContext context) {
        final JsonObject parameterJson = new JsonObject();
        parameterJson.addProperty("name", "...");

        final JsonArray parametersJson = new JsonArray();
        parametersJson.add(parameterJson);

        final JsonObject methodGroupJson = new JsonObject();
        methodGroupJson.addProperty("name", methodGroup.name());
        methodGroupJson.add("parameters", parametersJson);

        return methodGroupJson;
    }
}
