/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.fabric;

import com.mojang.math.Transformation;
import li.cil.oc2.api.API;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public final class BusCableModel {
    public static final ResourceLocation BUS_CABLE_BASE_MODEL = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_base");
    public static final ResourceLocation BUS_CABLE_STRAIGHT_MODEL = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_straight");
    public static final ResourceLocation BUS_CABLE_SUPPORT_MODEL = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_support");

    // --------------------------------------------------------------------- //

    public static BusCableBakedModel bake(final BakedModel proxy, final ModelBaker baker, final ModelState modelState) {
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

        return new BusCableBakedModel(proxy, straightModelByAxis, supportModelByFace);
    }

    // --------------------------------------------------------------------- //

    private static BakedModel bake(final ModelBaker baker, final ResourceLocation model,
                                   final ModelState modelState, @Nullable final BlockModelRotation rotation) {
        final ModelState state = rotation == null ? modelState : new ComposedModelState(
            modelState.getRotation().compose(rotation.getRotation()), modelState.isUvLocked());
        return requireNonNull(baker.bake(model, state));
    }

    private record ComposedModelState(Transformation rotation, boolean uvLocked) implements ModelState {
        @Override
        public Transformation getRotation() {
            return rotation;
        }

        @Override
        public boolean isUvLocked() {
            return uvLocked;
        }
    }

    private BusCableModel() {
    }
}
