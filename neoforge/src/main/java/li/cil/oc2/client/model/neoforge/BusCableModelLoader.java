/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.neoforge;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

import java.util.ArrayList;
import java.util.List;

public final class BusCableModelLoader implements IGeometryLoader<BusCableModel> {
    @Override
    public BusCableModel read(final JsonObject json, final JsonDeserializationContext context) {
        final List<BlockElement> elements = new ArrayList<>();
        if (json.has("elements")) {
            for (final JsonElement element : GsonHelper.getAsJsonArray(json, "elements")) {
                elements.add(context.deserialize(element, BlockElement.class));
            }
        }

        return new BusCableModel(elements);
    }
}
