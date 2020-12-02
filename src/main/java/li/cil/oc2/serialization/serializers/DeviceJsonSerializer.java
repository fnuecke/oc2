package li.cil.oc2.serialization.serializers;

import com.google.gson.*;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceMethod;

import java.lang.reflect.Type;

public final class DeviceJsonSerializer implements JsonSerializer<Device> {
    @Override
    public JsonElement serialize(final Device src, final Type typeOfSrc, final JsonSerializationContext context) {
        if (src == null) {
            return JsonNull.INSTANCE;
        }

        final JsonObject deviceJson = new JsonObject();
        deviceJson.add("deviceId", context.serialize(src.getUniqueIdentifier()));
        deviceJson.add("typeNames", context.serialize(src.getTypeNames()));

        final JsonArray methodsJson = new JsonArray();
        deviceJson.add("methods", methodsJson);
        for (final DeviceMethod method : src.getMethods()) {
            methodsJson.add(context.serialize(method, DeviceMethod.class));
        }

        return deviceJson;
    }
}
