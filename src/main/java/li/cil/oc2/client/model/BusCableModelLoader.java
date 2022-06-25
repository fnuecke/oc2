/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;

public final class BusCableModelLoader implements IModelLoader<BusCableModel> {
    @Override
    public void onResourceManagerReload(final ResourceManager resourceManager) {
    }

    @Override
    public BusCableModel read(final JsonDeserializationContext context, final JsonObject modelContents) {
        return new BusCableModel(ModelLoaderRegistry.VanillaProxy.Loader.INSTANCE.read(context, modelContents));
    }
}
