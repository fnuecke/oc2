/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import li.cil.oc2.common.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.lang.reflect.Type;

public final class FluidStackJsonSerializer implements JsonSerializer<FluidStack> {
    @Override
    public JsonElement serialize(final FluidStack src, final Type typeOfSrc, final JsonSerializationContext context) {
        if (src.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        final JsonObject json = new JsonObject();
        json.addProperty("id", BuiltInRegistries.FLUID.getKey(src.fluid()).toString());
        json.addProperty("amount", src.amount());
        return json;
    }
}
