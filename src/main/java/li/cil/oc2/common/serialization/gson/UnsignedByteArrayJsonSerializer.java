package li.cil.oc2.common.serialization.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public final class UnsignedByteArrayJsonSerializer implements JsonSerializer<byte[]> {
    @Override
    public JsonElement serialize(final byte[] src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonArray json = new JsonArray();
        for (final byte b : src) {
            json.add(b & 0xFF);
        }
        return json;
    }
}
