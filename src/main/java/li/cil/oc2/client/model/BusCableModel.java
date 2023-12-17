/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model;

import com.mojang.datafixers.util.Pair;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class BusCableModel implements IUnbakedGeometry<BusCableModel> {
    private static final ResourceLocation BUS_CABLE_STRAIGHT_MODEL = new ResourceLocation(API.MOD_ID, "block/cable_straight");
    private static final ResourceLocation BUS_CABLE_SUPPORT_MODEL = new ResourceLocation(API.MOD_ID, "block/cable_support");

    private final IUnbakedGeometry<?> proxy;

    BusCableModel(final IUnbakedGeometry<?> proxy) {
        this.proxy = proxy;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public BakedModel bake(final IGeometryBakingContext context, final ModelBakery bakery, final Function<Material, TextureAtlasSprite> spriteGetter, final ModelState modelState, final ItemOverrides overrides, final ResourceLocation modelLocation) {
        final BakedModel bakedBaseModel = proxy.bake(context, bakery, spriteGetter, modelState, overrides, modelLocation);
        final BakedModel[] straightModelByAxis = {
            requireNonNull(bakery.bake(BUS_CABLE_STRAIGHT_MODEL, new CompositeModelState(modelState, BlockModelRotation.X0_Y90), spriteGetter)),
            requireNonNull(bakery.bake(BUS_CABLE_STRAIGHT_MODEL, new CompositeModelState(modelState, BlockModelRotation.X90_Y0), spriteGetter)),
            requireNonNull(bakery.bake(BUS_CABLE_STRAIGHT_MODEL, modelState, spriteGetter))
        };
        final BakedModel[] supportModelByFace = {
            requireNonNull(bakery.bake(BUS_CABLE_SUPPORT_MODEL, new CompositeModelState(modelState, BlockModelRotation.X270_Y0), spriteGetter)), // -y
            requireNonNull(bakery.bake(BUS_CABLE_SUPPORT_MODEL, new CompositeModelState(modelState, BlockModelRotation.X90_Y0), spriteGetter)), // +y
            requireNonNull(bakery.bake(BUS_CABLE_SUPPORT_MODEL, new CompositeModelState(modelState, BlockModelRotation.X0_Y180), spriteGetter)), // -z
            requireNonNull(bakery.bake(BUS_CABLE_SUPPORT_MODEL, modelState, spriteGetter)), // +z
            requireNonNull(bakery.bake(BUS_CABLE_SUPPORT_MODEL, new CompositeModelState(modelState, BlockModelRotation.X0_Y90), spriteGetter)), // -x
            requireNonNull(bakery.bake(BUS_CABLE_SUPPORT_MODEL, new CompositeModelState(modelState, BlockModelRotation.X0_Y270), spriteGetter)) // +x
        };

        return new BusCableBakedModel(bakedBaseModel, straightModelByAxis, supportModelByFace);
    }

    @Override
    public Collection<Material> getMaterials(final IGeometryBakingContext context, final Function<ResourceLocation, UnbakedModel> modelGetter, final Set<Pair<String, String>> missingTextureErrors) {
        final ArrayList<Material> materials = new ArrayList<>(proxy.getMaterials(context, modelGetter, missingTextureErrors));
        materials.addAll(modelGetter.apply(BUS_CABLE_STRAIGHT_MODEL).getMaterials(modelGetter, missingTextureErrors));
        materials.addAll(modelGetter.apply(BUS_CABLE_SUPPORT_MODEL).getMaterials(modelGetter, missingTextureErrors));
        return materials;
    }
}
