/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.common.blockentity.ChargerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class ChargerRenderer implements BlockEntityRenderer<ChargerBlockEntity, ChargerRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private boolean hasEnergy;
        private long gameTime;
        private float partialTicks;
    }

    // --------------------------------------------------------------------- //

    public static final Identifier EFFECT_LOCATION = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/charger/effect");

    private static final Material TEXTURE_EFFECT = new Material(TextureAtlas.LOCATION_BLOCKS, EFFECT_LOCATION);

    private static final int EFFECT_LAYERS = 3;
    private static final float EFFECT_HEIGHT = 0.5f;
    private static final float EFFECT_SPEED = 0.3f;
    private static final float EFFECT_SCALE_START = 0.6f;
    private static final float EFFECT_SCALE_END = 0.8f;

    private static final Vector3f INDICATOR_COLOR = new Vector3f(1f, 0.8f, 0.3f);
    private static final Vector3f INDICATOR_COLOR_BRIGHT = new Vector3f(1f, 1f, 0.7f);

    private static final AABB INDICATOR_BOUNDS = new AABB(0.5 / 16.0, 12 / 16.0, 0.5 / 16.0, 15.5 / 16.0, 15.5 / 16.0, 15.5 / 16.0);

    private static final RenderType EFFECT_RENDER_TYPE = TEXTURE_EFFECT.renderType(ModRenderType::getUnlitBlock);

    // --------------------------------------------------------------------- //

    public ChargerRenderer(final BlockEntityRendererProvider.Context ignoredContext) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final ChargerBlockEntity charger, final State state, final float partialTicks, final Vec3 cameraPosition,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(charger, state, partialTicks, cameraPosition, crumbling);

        final Level level = charger.getLevel();
        state.hasEnergy = charger.hasEnergy();
        state.gameTime = level != null ? level.getGameTime() : 0;
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        if (!state.hasEnergy) {
            return;
        }

        final long gameTime = state.gameTime;
        final float partialTicks = state.partialTicks;
        final float offset = (gameTime + partialTicks) * EFFECT_SPEED / 20f % (float) (Math.PI * 2);

        Overlays.renderBox(stack, collector, INDICATOR_BOUNDS, INDICATOR_COLOR, INDICATOR_COLOR_BRIGHT, gameTime, partialTicks);

        stack.pushPose();
        stack.translate(0.5, 1.1, 0.5);

        for (int i = 0; i < EFFECT_LAYERS; i++) {
            final float relativeY = (1 + Mth.sin(offset + ((float) Math.PI * 2f * i / EFFECT_LAYERS))) * 0.5f;
            final float y = relativeY * EFFECT_HEIGHT;
            final float scale = EFFECT_SCALE_START + relativeY * (EFFECT_SCALE_END - EFFECT_SCALE_START);

            stack.pushPose();
            stack.translate(0, y, 0);
            renderScaledQuad(stack, collector, scale);
            stack.popPose();
        }

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    private static void renderScaledQuad(final PoseStack stack, final SubmitNodeCollector collector, final float scale) {
        stack.pushPose();
        stack.scale(scale, scale, scale);
        collector.submitCustomGeometry(stack, EFFECT_RENDER_TYPE, ChargerRenderer::renderQuad);
        stack.popPose();
    }

    private static void renderQuad(final PoseStack.Pose pose, final VertexConsumer consumer) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(TEXTURE_EFFECT);
        consumer.addVertex(pose, -0.5f, 0, -0.5f).setUv(sprite.getU0(), sprite.getV0());
        consumer.addVertex(pose, -0.5f, 0, 0.5f).setUv(sprite.getU0(), sprite.getV1());
        consumer.addVertex(pose, 0.5f, 0, 0.5f).setUv(sprite.getU1(), sprite.getV1());
        consumer.addVertex(pose, 0.5f, 0, -0.5f).setUv(sprite.getU1(), sprite.getV0());
    }
}
