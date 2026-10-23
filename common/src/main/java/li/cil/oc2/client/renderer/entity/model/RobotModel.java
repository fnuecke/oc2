/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.api.API;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class RobotModel {
    public static final ModelLayerLocation ROBOT_MODEL_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "robot"), "main");
    public static final Identifier ROBOT_ENTITY_TEXTURE = Identifier.fromNamespaceAndPath(API.MOD_ID, "textures/entity/robot/robot.png");

    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(ROBOT_ENTITY_TEXTURE);

    // --------------------------------------------------------------------- //

    private final ModelPart root;
    private final ModelPart topRenderer;
    private final ModelPart baseRenderer;
    private final ModelPart coreRenderer;
    private final Quaternionf topRotationQuaternion = new Quaternionf();

    // --------------------------------------------------------------------- //

    public RobotModel(final ModelPart modelPart) {
        root = modelPart;
        topRenderer = modelPart.getChild("top");
        baseRenderer = modelPart.getChild("base");
        coreRenderer = modelPart.getChild("core");
    }

    public static LayerDefinition createRobotLayer() {
        final MeshDefinition meshDefinition = new MeshDefinition();
        final PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild("top", CubeListBuilder.create()
                .texOffs(1, 1)
                .addBox(-7, 8, -7, 14, 6, 14),
            PartPose.ZERO);
        partDefinition.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(1, 23)
                .addBox(-7, 0, -7, 14, 7, 14),
            PartPose.ZERO);
        partDefinition.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(1, 34)
                .addBox(-6, 7, -6, 12, 1, 12),
            PartPose.ZERO);
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    // --------------------------------------------------------------------- //

    public void applyTopTransform(final PoseStack stack, final float topY, final float topRotationY) {
        stack.translate(0, topY, 0);
        stack.mulPose(topRotationQuaternion.rotationXYZ(0, topRotationY * Mth.DEG_TO_RAD, 0));
    }

    public void getExtents(final PoseStack stack, final Consumer<Vector3fc> output) {
        root.getExtentsForGui(stack, output);
    }

    public void submit(final PoseStack stack, final SubmitNodeCollector collector, final int packedLight,
                       final float baseY, final float topY, final float topRotationY) {
        stack.pushPose();
        applyTopTransform(stack, topY, topRotationY);
        collector.submitModelPart(topRenderer, stack, RENDER_TYPE, packedLight, OverlayTexture.NO_OVERLAY, null);
        stack.popPose();

        stack.pushPose();
        stack.translate(0, baseY, 0);
        collector.submitModelPart(baseRenderer, stack, RENDER_TYPE, packedLight, OverlayTexture.NO_OVERLAY, null);
        collector.submitModelPart(coreRenderer, stack, RENDER_TYPE, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, null);
        stack.popPose();
    }
}
