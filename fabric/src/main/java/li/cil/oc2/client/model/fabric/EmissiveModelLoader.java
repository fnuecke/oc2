/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.fabric;

import li.cil.oc2.api.API;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class EmissiveModelLoader {
    private static final Set<ResourceLocation> EMISSIVE_MODELS = Set.of(
        ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/network_connector_indicator"),
        ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/transposer_glow"));

    // --------------------------------------------------------------------- //

    public static void initialize() {
        ModelLoadingPlugin.register(pluginContext ->
            pluginContext.modifyModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                final ResourceLocation id = context.resourceId();
                if (model == null || id == null || !EMISSIVE_MODELS.contains(id)) {
                    return model;
                }

                final RenderMaterial material = RendererAccess.INSTANCE.getRenderer().materialFinder()
                    .blendMode(BlendMode.CUTOUT)
                    .emissive(true)
                    .disableDiffuse(true)
                    .ambientOcclusion(TriState.FALSE)
                    .find();
                return new EmissiveBakedModel(model, material);
            }));
    }

    // --------------------------------------------------------------------- //

    private EmissiveModelLoader() {
    }
}
