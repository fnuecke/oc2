/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import li.cil.oc2.api.util.Side;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

public final class SideJsonDeserializer implements JsonDeserializer<Side> {
    @Override
    public Side deserialize(final JsonElement json, final Type typeOfT, @Nullable final JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            final JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
            if (jsonPrimitive.isNumber()) {
                try {
                    return Side.byIndex(jsonPrimitive.getAsNumber().intValue());
                } catch (final IllegalArgumentException e) {
                    throw new JsonParseException(e.getMessage());
                }
            }
        }

        return (Side) TypeAdapters.ENUM_FACTORY.create(null, TypeToken.get(typeOfT)).fromJsonTree(json);
    }
}
