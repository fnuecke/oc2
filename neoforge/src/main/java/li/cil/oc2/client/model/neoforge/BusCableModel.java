/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.neoforge;

import li.cil.oc2.api.API;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public record BusCableModel(List<BlockElement> elements) implements IUnbakedGeometry<BusCableModel> {
    private static final ResourceLocation BUS_CABLE_STRAIGHT_MODEL = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_straight");
    private static final ResourceLocation BUS_CABLE_SUPPORT_MODEL = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_support");

    // ------------------------------------------------------------- //

    @Override
    public BakedModel bake(final IGeometryBakingContext context, final ModelBaker baker,
                           final Function<Material, TextureAtlasSprite> spriteGetter,
                           final ModelState modelState, final ItemOverrides overrides) {
        final IModelBuilder<?> builder = IModelBuilder.of(
                context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(),
                context.getTransforms(), overrides,
                spriteGetter.apply(context.getMaterial("particle")), RenderTypeGroup.EMPTY);
        bakeElementsResolvingTextureRefs(builder, spriteGetter, modelState, context);
        final BakedModel bakedBaseModel = builder.build();

        final BakedModel[] straightModelByAxis = {
                bake(baker, BUS_CABLE_STRAIGHT_MODEL, modelState, BlockModelRotation.X0_Y90),
                bake(baker, BUS_CABLE_STRAIGHT_MODEL, modelState, BlockModelRotation.X90_Y0),
                bake(baker, BUS_CABLE_STRAIGHT_MODEL, modelState, null)
        };
        final BakedModel[] supportModelByFace = {
                bake(baker, BUS_CABLE_SUPPORT_MODEL, modelState, BlockModelRotation.X270_Y0), // -y
                bake(baker, BUS_CABLE_SUPPORT_MODEL, modelState, BlockModelRotation.X90_Y0), // +y
                bake(baker, BUS_CABLE_SUPPORT_MODEL, modelState, BlockModelRotation.X0_Y180), // -z
                bake(baker, BUS_CABLE_SUPPORT_MODEL, modelState, null), // +z
                bake(baker, BUS_CABLE_SUPPORT_MODEL, modelState, BlockModelRotation.X0_Y90), // -x
                bake(baker, BUS_CABLE_SUPPORT_MODEL, modelState, BlockModelRotation.X0_Y270) // +x
        };

        return new BusCableBakedModel(bakedBaseModel, straightModelByAxis, supportModelByFace);
    }

    @Override
    public void resolveParents(final Function<ResourceLocation, UnbakedModel> modelGetter, final IGeometryBakingContext context) {
        modelGetter.apply(BUS_CABLE_STRAIGHT_MODEL).resolveParents(modelGetter);
        modelGetter.apply(BUS_CABLE_SUPPORT_MODEL).resolveParents(modelGetter);
    }

    // ------------------------------------------------------------- //

    private void bakeElementsResolvingTextureRefs(final IModelBuilder<?> builder,
                                                  final Function<Material, TextureAtlasSprite> spriteGetter,
                                                  final ModelState modelState,
                                                  final IGeometryBakingContext context) {
        for (final BlockElement element : elements) {
            element.faces.forEach((side, face) -> {
                final TextureAtlasSprite sprite = spriteGetter.apply(context.getMaterial(face.texture()));
                final BakedQuad quad = UnbakedGeometryHelper.bakeElementFace(element, face, sprite, side, modelState);
                if (face.cullForDirection() == null) {
                    builder.addUnculledFace(quad);
                } else {
                    builder.addCulledFace(Direction.rotate(modelState.getRotation().getMatrix(), face.cullForDirection()), quad);
                }
            });
        }
    }

    private static BakedModel bake(final ModelBaker baker, final ResourceLocation model,
                                   final ModelState modelState, final BlockModelRotation rotation) {
        final ModelState state = rotation == null ? modelState : new SimpleModelState(
                modelState.getRotation().compose(rotation.getRotation()), modelState.isUvLocked());
        return requireNonNull(baker.bake(model, state));
    }
}
