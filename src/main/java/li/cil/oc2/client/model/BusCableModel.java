package li.cil.oc2.client.model;

import com.mojang.datafixers.util.Pair;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelTransformComposition;
import net.minecraftforge.client.model.geometry.IModelGeometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class BusCableModel implements IModelGeometry<BusCableModel> {
    private static final ResourceLocation BUS_CABLE_STRAIGHT_MODEL = new ResourceLocation(API.MOD_ID, "block/cable_straight");
    private static final ResourceLocation BUS_CABLE_SUPPORT_MODEL = new ResourceLocation(API.MOD_ID, "block/cable_support");

    ///////////////////////////////////////////////////////////////////

    private final ModelLoaderRegistry.VanillaProxy proxy;

    ///////////////////////////////////////////////////////////////////

    public BusCableModel(final ModelLoaderRegistry.VanillaProxy proxy) {
        this.proxy = proxy;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public IBakedModel bake(final IModelConfiguration owner, final ModelBakery bakery, final Function<RenderMaterial, TextureAtlasSprite> spriteGetter, final IModelTransform modelTransform, final ItemOverrideList overrides, final ResourceLocation modelLocation) {
        final IBakedModel bakedBaseModel = proxy.bake(owner, bakery, spriteGetter, modelTransform, overrides, modelLocation);
        final IBakedModel[] straightModelByAxis = {
                requireNonNull(bakery.getBakedModel(BUS_CABLE_STRAIGHT_MODEL, new ModelTransformComposition(modelTransform, ModelRotation.X0_Y90), spriteGetter)),
                requireNonNull(bakery.getBakedModel(BUS_CABLE_STRAIGHT_MODEL, new ModelTransformComposition(modelTransform, ModelRotation.X90_Y0), spriteGetter)),
                requireNonNull(bakery.getBakedModel(BUS_CABLE_STRAIGHT_MODEL, modelTransform, spriteGetter))
        };
        final IBakedModel[] supportModelByFace = {
                requireNonNull(bakery.getBakedModel(BUS_CABLE_SUPPORT_MODEL, new ModelTransformComposition(modelTransform, ModelRotation.X270_Y0), spriteGetter)), // -y
                requireNonNull(bakery.getBakedModel(BUS_CABLE_SUPPORT_MODEL, new ModelTransformComposition(modelTransform, ModelRotation.X90_Y0), spriteGetter)), // +y
                requireNonNull(bakery.getBakedModel(BUS_CABLE_SUPPORT_MODEL, new ModelTransformComposition(modelTransform, ModelRotation.X0_Y180), spriteGetter)), // -z
                requireNonNull(bakery.getBakedModel(BUS_CABLE_SUPPORT_MODEL, modelTransform, spriteGetter)), // +z
                requireNonNull(bakery.getBakedModel(BUS_CABLE_SUPPORT_MODEL, new ModelTransformComposition(modelTransform, ModelRotation.X0_Y90), spriteGetter)), // -x
                requireNonNull(bakery.getBakedModel(BUS_CABLE_SUPPORT_MODEL, new ModelTransformComposition(modelTransform, ModelRotation.X0_Y270), spriteGetter)) // +x
        };

        return new BusCableBakedModel(bakedBaseModel, straightModelByAxis, supportModelByFace);
    }

    @Override
    public Collection<RenderMaterial> getTextures(final IModelConfiguration owner, final Function<ResourceLocation, IUnbakedModel> modelGetter, final Set<Pair<String, String>> missingTextureErrors) {
        final ArrayList<RenderMaterial> textures = new ArrayList<>(proxy.getTextures(owner, modelGetter, missingTextureErrors));
        textures.addAll(modelGetter.apply(BUS_CABLE_STRAIGHT_MODEL).getMaterials(modelGetter, missingTextureErrors));
        textures.addAll(modelGetter.apply(BUS_CABLE_SUPPORT_MODEL).getMaterials(modelGetter, missingTextureErrors));
        return textures;
    }
}
