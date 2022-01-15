package li.cil.oc2.common.serialization.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import li.cil.oc2.common.bus.RPCDeviceBusAdapter;

import java.lang.reflect.Type;

public final class RPCDeviceWithIdentifierJsonSerializer implements JsonSerializer<RPCDeviceBusAdapter.RPCDeviceWithIdentifier> {
    @Override
    public JsonElement serialize(final RPCDeviceBusAdapter.RPCDeviceWithIdentifier src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonObject deviceJson = new JsonObject();
        deviceJson.add("deviceId", context.serialize(src.identifier()));
        deviceJson.add("typeNames", context.serialize(src.device().getTypeNames()));

        return deviceJson;
    }
}
