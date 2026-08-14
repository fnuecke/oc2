/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import li.cil.oc2.api.util.Side;

import java.lang.reflect.Type;

public final class SideJsonDeserializer implements JsonDeserializer<Side> {
    private static final Side[] SIDES = Side.values();

    @Override
    public Side deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            final JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
            if (jsonPrimitive.isNumber()) {
                final int ordinal = jsonPrimitive.getAsNumber().intValue();
                if (ordinal < 0 || ordinal >= SIDES.length) {
                    throw new JsonParseException("side out of range: " + ordinal
                            + " (expected 0 to " + (SIDES.length - 1) + ")");
                }
                return SIDES[ordinal];
            }
        }

        return (Side) TypeAdapters.ENUM_FACTORY.create(null, TypeToken.get(typeOfT)).fromJsonTree(json);
    }
}
