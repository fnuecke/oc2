package li.cil.oc2.common.serialization.serializers;

import com.google.gson.*;

import java.lang.reflect.Type;

public final class UnsignedByteArrayJsonSerializer implements JsonSerializer<byte[]> {
    @Override
    public JsonElement serialize(final byte[] src, final Type typeOfSrc, final JsonSerializationContext context) {
        if (src == null) {
            return JsonNull.INSTANCE;
        }

        final JsonArray json = new JsonArray();
        for (final byte b : src) {
            json.add(b & 0xFF);
        }
        return json;
    }
}
