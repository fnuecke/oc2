package li.cil.oc2.serialization.serializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import li.cil.ceres.api.*;

import javax.annotation.Nullable;

@RegisterSerializer
public final class JsonArraySerializer implements Serializer<JsonArray> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<JsonArray> type, final Object value) throws SerializationException {
        final JsonArray jsonArray = (JsonArray) value;
        visitor.putObject("value", String.class, jsonArray.toString());
    }

    @Override
    public JsonArray deserialize(final DeserializationVisitor visitor, final Class<JsonArray> type, @Nullable final Object value) throws SerializationException {
        JsonArray array = (JsonArray) value;
        if (!visitor.exists("value")) {
            return array;
        }

        final String jsonString = (String) visitor.getObject("value", String.class, null);
        if (jsonString == null) {
            return null;
        }

        if (array == null) {
            array = new JsonArray();
        }

        while (array.size() > 0) {
            array.remove(array.size() - 1);
        }

        array.addAll(new JsonParser().parse(jsonString).getAsJsonArray());

        return array;
    }
}
