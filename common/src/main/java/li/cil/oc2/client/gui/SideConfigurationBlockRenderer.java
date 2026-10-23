/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.client.gui.widget.Texture;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public final class SideConfigurationBlockRenderer extends PictureInPictureRenderer<SideConfigurationBlockRenderer.State> {
    public record State(Quaternionf rotation, int enabledSides, @Nullable Direction focusedSide,
                        int x0, int y0, int x1, int y1, float scale,
                        @Nullable ScreenRectangle scissorArea,
                        @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        public State(final Quaternionf rotation, final int enabledSides, @Nullable final Direction focusedSide,
                     final int x0, final int y0, final int x1, final int y1, final float scale) {
            this(rotation, enabledSides, focusedSide, x0, y0, x1, y1, scale, null, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null));
        }
    }

    // --------------------------------------------------------------------- //

    private final ItemStack computerItemStack = new ItemStack(Items.COMPUTER.get());
    private final ItemStackRenderState computerRenderState = new ItemStackRenderState();

    // --------------------------------------------------------------------- //

    public SideConfigurationBlockRenderer(final MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    // --------------------------------------------------------------------- //

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void renderToTexture(final State state, final PoseStack poseStack) {
        poseStack.scale(1, 1, -1);
        poseStack.mulPose(state.rotation());
        poseStack.scale(1, -1, 1);

        renderBlock(poseStack);
        renderOverlays(state, poseStack);
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale) {
        return height / 2f;
    }

    @Override
    protected String getTextureLabel() {
        return "oc2 side configuration";
    }

    // --------------------------------------------------------------------- //

    private void renderBlock(final PoseStack poseStack) {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        minecraft.getItemModelResolver().updateForTopItem(computerRenderState, computerItemStack, ItemDisplayContext.NONE, null, null, 0);

        final FeatureRenderDispatcher featureRenderDispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
        computerRenderState.submit(poseStack, featureRenderDispatcher.getSubmitNodeStorage(), 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        featureRenderDispatcher.renderAllFeatures();
    }

    private void renderOverlays(final State state, final PoseStack poseStack) {
        for (final Direction side : Direction.values()) {
            if (side == Direction.SOUTH) {
                continue;
            }

            poseStack.pushPose();

            poseStack.translate(-side.getStepX() * 0.51, side.getStepY() * 0.51, -side.getStepZ() * 0.51);

            final Vector3f sideRotation = switch (side) {
                case DOWN -> new Vector3f(-90, 0, 0);
                case UP -> new Vector3f(90, 0, 0);
                case NORTH -> new Vector3f(0, 180, 0);
                case WEST -> new Vector3f(0, -90, 0);
                case EAST -> new Vector3f(0, 90, 0);
                default -> throw new IllegalStateException("Unexpected value: " + side);
            };
            poseStack.mulPose(new Quaternionf().rotationXYZ(
                (float) Math.toRadians(sideRotation.x()),
                (float) Math.toRadians(sideRotation.y()),
                (float) Math.toRadians(sideRotation.z())));

            poseStack.translate(-0.5, -0.5, 0);

            if ((state.enabledSides() & (1 << side.get3DDataValue())) != 0) {
                renderOverlay(poseStack, Textures.BLOCK_FACE_ENABLED_TEXTURE);
            } else {
                renderOverlay(poseStack, Textures.BLOCK_FACE_DISABLED_TEXTURE);
            }

            if (side == state.focusedSide()) {
                renderOverlay(poseStack, Textures.BLOCK_FACE_FOCUSED_TEXTURE);
            }

            poseStack.popPose();
        }
    }

    private void renderOverlay(final PoseStack poseStack, final Texture texture) {
        final VertexConsumer buffer = bufferSource.getBuffer(ModRenderType.getOverlay(texture.location));

        buffer.addVertex(poseStack.last().pose(), 0, 0, 0).setUv(0, 0);
        buffer.addVertex(poseStack.last().pose(), 0, 1, 0).setUv(0, 1);
        buffer.addVertex(poseStack.last().pose(), 1, 1, 0).setUv(1, 1);
        buffer.addVertex(poseStack.last().pose(), 1, 0, 0).setUv(1, 0);
    }
}
