package li.cil.oc2.client.renderer.tileentity;

import com.mojang.blaze3d.matrix.PoseStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.CustomRenderType;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.tileentity.ComputerBlockEntity;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.tileentity.BlockEntityRenderer;
import net.minecraft.client.renderer.tileentity.BlockEntityRendererDispatcher;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;

import java.util.List;

public final class ComputerBlockEntityRenderer extends BlockEntityRenderer<ComputerBlockEntity> {
    public static final ResourceLocation OVERLAY_POWER_LOCATION = new ResourceLocation(API.MOD_ID, "block/computer/computer_overlay_power");
    public static final ResourceLocation OVERLAY_STATUS_LOCATION = new ResourceLocation(API.MOD_ID, "block/computer/computer_overlay_status");
    public static final ResourceLocation OVERLAY_TERMINAL_LOCATION = new ResourceLocation(API.MOD_ID, "block/computer/computer_overlay_terminal");

    private static final RenderMaterial TEXTURE_POWER = new RenderMaterial(PlayerContainer.BLOCK_ATLAS, OVERLAY_POWER_LOCATION);
    private static final RenderMaterial TEXTURE_STATUS = new RenderMaterial(PlayerContainer.BLOCK_ATLAS, OVERLAY_STATUS_LOCATION);
    private static final RenderMaterial TEXTURE_TERMINAL = new RenderMaterial(PlayerContainer.BLOCK_ATLAS, OVERLAY_TERMINAL_LOCATION);

    ///////////////////////////////////////////////////////////////////

    public ComputerBlockEntityRenderer(final BlockEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final ComputerBlockEntity tileEntity, final float partialTicks, final PoseStack matrixStack, final IRenderTypeBuffer buffer, final int light, final int overlay) {
        final Direction blockFacing = tileEntity.getBlockState().getValue(ComputerBlock.FACING);
        final Vector3d cameraPosition = renderer.camera.getEntity().getEyePosition(partialTicks);

        // If viewer is not in front of the block we can skip all of the rest, it cannot be visible.
        // We check against the center of the block instead of the actual relevant face for simplicity.
        final Vector3d relativeCameraPosition = cameraPosition.subtract(Vector3d.atCenterOf(tileEntity.getBlockPos()));
        final double projectedCameraPosition = relativeCameraPosition.dot(Vector3d.atLowerCornerOf(blockFacing.getNormal()));
        if (projectedCameraPosition <= 0) {
            return;
        }

        matrixStack.pushPose();

        // Align with front face of block.
        final Quaternion rotation = new Quaternion(Vector3f.YN, blockFacing.toYRot() + 180, true);
        matrixStack.translate(0.5f, 0, 0.5f);
        matrixStack.mulPose(rotation);
        matrixStack.translate(-0.5f, 0, -0.5f);

        // Flip and align with top left corner.
        matrixStack.translate(1, 1, 0);
        matrixStack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        matrixStack.scale(pixelScale, pixelScale, pixelScale);

        if (tileEntity.getVirtualMachine().isRunning()) {
            renderTerminal(tileEntity, matrixStack, buffer, cameraPosition);
        } else {
            renderStatusText(tileEntity, matrixStack, cameraPosition);
        }

        matrixStack.translate(0, 0, -0.1f);
        final Matrix4f matrix = matrixStack.last().pose();

        switch (tileEntity.getVirtualMachine().getBusState()) {
            case SCAN_PENDING:
            case INCOMPLETE:
                renderStatus(matrix, buffer);
                break;
            case TOO_COMPLEX:
                renderStatus(matrix, buffer, 1000);
                break;
            case MULTIPLE_CONTROLLERS:
                renderStatus(matrix, buffer, 250);
                break;
            case READY:
                switch (tileEntity.getVirtualMachine().getRunState()) {
                    case STOPPED:
                        break;
                    case LOADING_DEVICES:
                        renderStatus(matrix, buffer);
                        break;
                    case RUNNING:
                        renderPower(matrix, buffer);
                        break;
                }
                break;
        }

        matrixStack.popPose();
    }

    ///////////////////////////////////////////////////////////////////

    private void renderTerminal(final ComputerBlockEntity tileEntity, final PoseStack stack, final IRenderTypeBuffer buffer, final Vector3d cameraPosition) {
        // Render terminal content if close enough.
        if (Vector3d.atCenterOf(tileEntity.getBlockPos()).closerThan(cameraPosition, 6f)) {
            stack.pushPose();
            stack.translate(2, 2, -0.9f);

            // Scale to make terminal fit fully.
            final Terminal terminal = tileEntity.getTerminal();
            final float textScaleX = 12f / terminal.getWidth();
            final float textScaleY = 7f / terminal.getHeight();
            final float scale = Math.min(textScaleX, textScaleY) * 0.95f;

            // Center it on both axes.
            final float scaleDeltaX = textScaleX - scale;
            final float scaleDeltaY = textScaleY - scale;
            stack.translate(
                    terminal.getWidth() * scaleDeltaX * 0.5f,
                    terminal.getHeight() * scaleDeltaY * 0.5f,
                    0f);

            stack.scale(scale, scale, 1f);

            // TODO Make terminal renderer use buffer+rendertype.
            GlStateManager._enableBlend();
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask(false);
            terminal.render(stack);

            stack.popPose();
        } else {
            stack.pushPose();
            stack.translate(0, 0, -0.9f);

            final Matrix4f matrix = stack.last().pose();
            renderQuad(matrix, TEXTURE_TERMINAL.buffer(buffer, CustomRenderType::getUnlitBlock));

            stack.popPose();
        }
    }

    private void renderStatusText(final ComputerBlockEntity tileEntity, final PoseStack stack, final Vector3d cameraPosition) {
        if (!Vector3d.atCenterOf(tileEntity.getBlockPos()).closerThan(cameraPosition, 12f)) {
            return;
        }

        final ITextComponent bootError = tileEntity.getVirtualMachine().getBootError();
        if (bootError == null) {
            return;
        }

        stack.pushPose();
        stack.translate(3, 3, -0.9f);

        drawText(stack, bootError);

        stack.popPose();
    }

    private void drawText(final PoseStack stack, final ITextComponent text) {
        final int maxWidth = 100;

        stack.pushPose();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final FontRenderer fontRenderer = renderer.font;
        final List<ITextProperties> wrappedText = fontRenderer.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = fontRenderer.width(text);
            fontRenderer.draw(stack, text, (maxWidth - textWidth) * 0.5f, 0, 0xEE3322);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                fontRenderer.draw(stack, wrappedText.get(i).getString(), 0, i * fontRenderer.lineHeight, 0xEE3322);
            }
        }

        stack.popPose();
    }

    private void renderStatus(final Matrix4f matrix, final IRenderTypeBuffer buffer) {
        renderStatus(matrix, buffer, 0);
    }

    private void renderStatus(final Matrix4f matrix, final IRenderTypeBuffer buffer, final int frequency) {
        if (frequency <= 0 || (((System.currentTimeMillis() + hashCode()) / frequency) % 2) == 1) {
            renderQuad(matrix, TEXTURE_STATUS.buffer(buffer, CustomRenderType::getUnlitBlock));
        }
    }

    private void renderPower(final Matrix4f matrix, final IRenderTypeBuffer buffer) {
        renderQuad(matrix, TEXTURE_POWER.buffer(buffer, CustomRenderType::getUnlitBlock));
    }

    private static void renderQuad(final Matrix4f matrix, final IVertexBuilder builder) {
        // NB: We may get a SpriteAwareVertexBuilder here. Sadly, its chaining is broken,
        //     because methods may return the underlying vertex builder, so e.g. calling
        //     buffer.pos(...).tex(...) will not actually call SpriteAwareVertexBuilder.tex(...)
        //     but SpriteAwareVertexBuilder.vertexBuilder.tex(...), skipping the UV remapping.
        builder.vertex(matrix, 0, 0, 0);
        builder.uv(0, 0);
        builder.endVertex();

        builder.vertex(matrix, 0, 16, 0);
        builder.uv(0, 1);
        builder.endVertex();

        builder.vertex(matrix, 16, 16, 0);
        builder.uv(1, 1);
        builder.endVertex();

        builder.vertex(matrix, 16, 0, 0);
        builder.uv(1, 0);
        builder.endVertex();
    }
}
