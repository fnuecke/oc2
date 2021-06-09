package li.cil.oc2.common.serialization.serializers;

import com.google.gson.*;
import net.minecraft.util.Direction;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

public final class DirectionJsonSerializer implements JsonDeserializer<Direction>, JsonSerializer<Direction> {
    @Nullable
    @Override
    public Direction deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonPrimitive()) {
            return null;
        }

        final JsonPrimitive primitive = json.getAsJsonPrimitive();

        if (primitive.isString()) {
            final Direction direction = Direction.byName(json.getAsString());
            if (direction != null) {
                return direction;
            }
        }

        if (primitive.isNumber()) {
            return Direction.from3DDataValue(json.getAsInt());
        }

        return null;
    }

    @Override
    public JsonElement serialize(final Direction src, final Type typeOfSrc, final JsonSerializationContext context) {
        if (src == null) {
            return JsonNull.INSTANCE;
        } else {
            return new JsonPrimitive(src.toString());
        }
    }
}
