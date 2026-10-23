/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.blockentity.TransposerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;

public final class TransposerRenderer implements BlockEntityRenderer<TransposerBlockEntity, BlockEntityRenderState> {
    private static final Material TOP = material("top");
    private static final Material SIDE = material("side");
    private static final Material BOTTOM = material("bottom");

    private static final float NEAR = 0.5f / 16f;
    private static final float FAR = 15.5f / 16f;

    private static final RenderType RENDER_TYPE = TOP.renderType(ModRenderType::getUnlitBlock);

    // --------------------------------------------------------------------- //

    public TransposerRenderer(final BlockEntityRendererProvider.Context ignoredContext) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void submit(final BlockEntityRenderState state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        collector.submitCustomGeometry(stack, RENDER_TYPE, TransposerRenderer::render);
    }

    // --------------------------------------------------------------------- //

    private static void render(final PoseStack.Pose pose, final VertexConsumer consumer) {
        final MaterialSet materials = Minecraft.getInstance().getAtlasManager();

        final TextureAtlasSprite top = materials.get(TOP);
        quad(pose, consumer, top, 0, FAR, 0, 0, FAR, 1, 1, FAR, 1, 1, FAR, 0);

        final TextureAtlasSprite bottom = materials.get(BOTTOM);
        quad(pose, consumer, bottom, 0, NEAR, 0, 0, NEAR, 1, 1, NEAR, 1, 1, NEAR, 0);

        final TextureAtlasSprite side = materials.get(SIDE);
        quad(pose, consumer, side, 0, 1, NEAR, 0, 0, NEAR, 1, 0, NEAR, 1, 1, NEAR);
        quad(pose, consumer, side, 0, 1, FAR, 0, 0, FAR, 1, 0, FAR, 1, 1, FAR);
        quad(pose, consumer, side, NEAR, 1, 0, NEAR, 0, 0, NEAR, 0, 1, NEAR, 1, 1);
        quad(pose, consumer, side, FAR, 1, 0, FAR, 0, 0, FAR, 0, 1, FAR, 1, 1);
    }

    private static Material material(final String name) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(API.MOD_ID, "block/transposer/" + name));
    }

    private static void quad(final PoseStack.Pose pose, final VertexConsumer consumer, final TextureAtlasSprite sprite,
                             final float x0, final float y0, final float z0,
                             final float x1, final float y1, final float z1,
                             final float x2, final float y2, final float z2,
                             final float x3, final float y3, final float z3) {
        consumer.addVertex(pose, x0, y0, z0).setUv(sprite.getU0(), sprite.getV0());
        consumer.addVertex(pose, x1, y1, z1).setUv(sprite.getU0(), sprite.getV1());
        consumer.addVertex(pose, x2, y2, z2).setUv(sprite.getU1(), sprite.getV1());
        consumer.addVertex(pose, x3, y3, z3).setUv(sprite.getU1(), sprite.getV0());
    }
}
