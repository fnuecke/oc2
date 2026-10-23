/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class RobotSpecialRenderer implements NoDataSpecialModelRenderer {
    private final RobotModel model;

    // --------------------------------------------------------------------- //

    public RobotSpecialRenderer(final RobotModel model) {
        this.model = model;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void submit(final ItemDisplayContext displayContext, final PoseStack poseStack, final SubmitNodeCollector collector,
                       final int packedLight, final int packedOverlay, final boolean hasFoil, final int outlineColor) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        model.submit(poseStack, collector, packedLight, 0, 0, 0);
        poseStack.popPose();
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        final PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5, 0, 0.5);
        model.getExtents(poseStack, output);
    }

    // --------------------------------------------------------------------- //

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(final SpecialModelRenderer.BakingContext context) {
            return new RobotSpecialRenderer(new RobotModel(context.entityModelSet().bakeLayer(RobotModel.ROBOT_MODEL_LAYER)));
        }
    }
}
