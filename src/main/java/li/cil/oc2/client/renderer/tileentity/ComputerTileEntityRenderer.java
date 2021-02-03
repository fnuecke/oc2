package li.cil.oc2.client.renderer.tileentity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.CustomRenderType;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
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

public final class ComputerTileEntityRenderer extends TileEntityRenderer<ComputerTileEntity> {
    public static final ResourceLocation OVERLAY_POWER_LOCATION = new ResourceLocation(API.MOD_ID, "block/computer/computer_overlay_power");
    public static final ResourceLocation OVERLAY_STATUS_LOCATION = new ResourceLocation(API.MOD_ID, "block/computer/computer_overlay_status");
    public static final ResourceLocation OVERLAY_TERMINAL_LOCATION = new ResourceLocation(API.MOD_ID, "block/computer/computer_overlay_terminal");

    private static final RenderMaterial TEXTURE_POWER = new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, OVERLAY_POWER_LOCATION);
    private static final RenderMaterial TEXTURE_STATUS = new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, OVERLAY_STATUS_LOCATION);
    private static final RenderMaterial TEXTURE_TERMINAL = new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, OVERLAY_TERMINAL_LOCATION);

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntityRenderer(final TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final ComputerTileEntity tileEntity, final float partialTicks, final MatrixStack matrixStack, final IRenderTypeBuffer buffer, final int light, final int overlay) {
        final Direction blockFacing = tileEntity.getBlockState().get(ComputerBlock.HORIZONTAL_FACING);
        final Vector3d cameraPosition = renderDispatcher.renderInfo.getRenderViewEntity().getEyePosition(partialTicks);

        // If viewer is not in front of the block we can skip all of the rest, it cannot be visible.
        // We check against the center of the block instead of the actual relevant face for simplicity.
        final Vector3d relativeCameraPosition = cameraPosition.subtract(Vector3d.copyCentered(tileEntity.getPos()));
        final double projectedCameraPosition = relativeCameraPosition.dotProduct(Vector3d.copy(blockFacing.getDirectionVec()));
        if (projectedCameraPosition <= 0) {
            return;
        }

        matrixStack.push();

        // Align with front face of block.
        final Quaternion rotation = new Quaternion(Vector3f.YN, blockFacing.getHorizontalAngle() + 180, true);
        matrixStack.translate(0.5f, 0, 0.5f);
        matrixStack.rotate(rotation);
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
        final Matrix4f matrix = matrixStack.getLast().getMatrix();

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

        matrixStack.pop();
    }

    ///////////////////////////////////////////////////////////////////

    private void renderTerminal(final ComputerTileEntity tileEntity, final MatrixStack stack, final IRenderTypeBuffer buffer, final Vector3d cameraPosition) {
        // Render terminal content if close enough.
        if (Vector3d.copyCentered(tileEntity.getPos()).isWithinDistanceOf(cameraPosition, 6f)) {
            stack.push();
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
            GlStateManager.enableBlend();
            GlStateManager.enableDepthTest();
            GlStateManager.depthMask(false);
            terminal.render(stack);

            stack.pop();
        } else {
            stack.push();
            stack.translate(0, 0, -0.9f);

            final Matrix4f matrix = stack.getLast().getMatrix();
            renderQuad(matrix, TEXTURE_TERMINAL.getBuffer(buffer, CustomRenderType::getUnlitBlock));

            stack.pop();
        }
    }

    private void renderStatusText(final ComputerTileEntity tileEntity, final MatrixStack stack, final Vector3d cameraPosition) {
        if (!Vector3d.copyCentered(tileEntity.getPos()).isWithinDistanceOf(cameraPosition, 12f)) {
            return;
        }

        final ITextComponent bootError = tileEntity.getVirtualMachine().getBootError();
        if (bootError == null) {
            return;
        }

        stack.push();
        stack.translate(3, 3, -0.9f);

        drawText(stack, bootError);

        stack.pop();
    }

    private void drawText(final MatrixStack stack, final ITextComponent text) {
        final int maxWidth = 100;

        stack.push();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final FontRenderer fontRenderer = renderDispatcher.getFontRenderer();
        final List<ITextProperties> wrappedText = renderDispatcher.getFontRenderer().getCharacterManager().func_238362_b_(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = fontRenderer.getStringPropertyWidth(text);
            fontRenderer.func_243248_b(stack, text, (maxWidth - textWidth) * 0.5f, 0, 0xEE3322);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                fontRenderer.drawString(stack, wrappedText.get(i).getString(), 0, i * fontRenderer.FONT_HEIGHT, 0xEE3322);
            }
        }

        stack.pop();
    }

    private void renderStatus(final Matrix4f matrix, final IRenderTypeBuffer buffer) {
        renderStatus(matrix, buffer, 0);
    }

    private void renderStatus(final Matrix4f matrix, final IRenderTypeBuffer buffer, final int frequency) {
        if (frequency <= 0 || (((System.currentTimeMillis() + hashCode()) / frequency) % 2) == 1) {
            renderQuad(matrix, TEXTURE_STATUS.getBuffer(buffer, CustomRenderType::getUnlitBlock));
        }
    }

    private void renderPower(final Matrix4f matrix, final IRenderTypeBuffer buffer) {
        renderQuad(matrix, TEXTURE_POWER.getBuffer(buffer, CustomRenderType::getUnlitBlock));
    }

    private static void renderQuad(final Matrix4f matrix, final IVertexBuilder buffer) {
        // NB: We may get a SpriteAwareVertexBuilder here. Sadly, its chaining is broken,
        //     because methods may return the underlying vertex builder, so e.g. calling
        //     buffer.pos(...).tex(...) will not actually call SpriteAwareVertexBuilder.tex(...)
        //     but SpriteAwareVertexBuilder.vertexBuilder.tex(...), skipping the UV remapping.
        buffer.pos(matrix, 0, 0, 0);
        buffer.tex(0, 0);
        buffer.endVertex();
        buffer.pos(matrix, 0, 16, 0);
        buffer.tex(0, 1);
        buffer.endVertex();
        buffer.pos(matrix, 16, 16, 0);
        buffer.tex(1, 1);
        buffer.endVertex();
        buffer.pos(matrix, 16, 0, 0);
        buffer.tex(1, 0);
        buffer.endVertex();
    }
}
