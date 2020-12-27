package li.cil.oc2.client.render.tile;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.client.render.OpenComputersRenderType;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.block.entity.ComputerTileEntity;
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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ComputerTileEntityRenderer extends TileEntityRenderer<ComputerTileEntity> {
    private static final ResourceLocation OVERLAY_POWER_LOCATION = new ResourceLocation(API.MOD_ID, "blocks/computer/computer_overlay_power");
    private static final ResourceLocation OVERLAY_STATUS_LOCATION = new ResourceLocation(API.MOD_ID, "blocks/computer/computer_overlay_status");
    private static final ResourceLocation OVERLAY_TERMINAL_LOCATION = new ResourceLocation(API.MOD_ID, "blocks/computer/computer_overlay_terminal");

    private static final RenderMaterial TEXTURE_POWER = new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, OVERLAY_POWER_LOCATION);
    private static final RenderMaterial TEXTURE_STATUS = new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, OVERLAY_STATUS_LOCATION);
    private static final RenderMaterial TEXTURE_TERMINAL = new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, OVERLAY_TERMINAL_LOCATION);

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void handleTextureStitchEvent(final TextureStitchEvent.Pre event) {
        event.addSprite(OVERLAY_POWER_LOCATION);
        event.addSprite(OVERLAY_STATUS_LOCATION);
        event.addSprite(OVERLAY_TERMINAL_LOCATION);
    }

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntityRenderer(final TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(final ComputerTileEntity tileEntity, final float partialTicks, final MatrixStack stack, final IRenderTypeBuffer buffer, final int combinedLightIn, final int combinedOverlayIn) {
        final Direction blockFacing = tileEntity.getBlockState().get(ComputerBlock.HORIZONTAL_FACING);
        final Vector3d cameraPosition = renderDispatcher.renderInfo.getRenderViewEntity().getEyePosition(partialTicks);

        // If viewer is not in front of the block we can skip all of the rest, it cannot be visible.
        // We check against the center of the block instead of the actual relevant face for simplicity.
        final Vector3d relativeCameraPosition = cameraPosition.subtract(Vector3d.copyCentered(tileEntity.getPos()));
        final double projectedCameraPosition = relativeCameraPosition.dotProduct(Vector3d.copy(blockFacing.getDirectionVec()));
        if (projectedCameraPosition <= 0) {
            return;
        }

        stack.push();

        // Align with front face of block.
        final Quaternion rotation = new Quaternion(Vector3f.YN, blockFacing.getHorizontalAngle() + 180, true);
        stack.translate(0.5f, 0, 0.5f);
        stack.rotate(rotation);
        stack.translate(-0.5f, 0, -0.5f);

        // Flip and align with top left corner.
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        if (tileEntity.isRunning()) {
            renderTerminal(tileEntity, stack, buffer, cameraPosition);
        } else {
            renderStatusText(tileEntity, stack, buffer, cameraPosition);
        }

        stack.translate(0, 0, -0.1f);
        final Matrix4f matrix = stack.getLast().getMatrix();

        switch (tileEntity.getBusState()) {
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
                switch (tileEntity.getRunState()) {
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

        stack.pop();
    }

    ///////////////////////////////////////////////////////////////////

    private void renderTerminal(final ComputerTileEntity tileEntity, final MatrixStack stack, final IRenderTypeBuffer buffer, final Vector3d cameraPosition) {
        // Render terminal content if close enough.
        if (Vector3d.copyCentered(tileEntity.getPos()).isWithinDistanceOf(cameraPosition, 6f * 6f)) {
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
            renderQuad(matrix, TEXTURE_TERMINAL.getBuffer(buffer, OpenComputersRenderType::getUnlitBlock));

            stack.pop();
        }
    }

    private void renderStatusText(final ComputerTileEntity tileEntity, final MatrixStack stack, final IRenderTypeBuffer buffer, final Vector3d cameraPosition) {
        if (!Vector3d.copyCentered(tileEntity.getPos()).isWithinDistanceOf(cameraPosition, 6f * 6f)) {
            return;
        }

        stack.push();
        stack.translate(3, 3, -0.9f);

        switch (tileEntity.getBusState()) {
            case SCAN_PENDING:
            case INCOMPLETE:
                drawText(stack, new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_INCOMPLETE), 0xFFFFFF);
                break;
            case TOO_COMPLEX:
                drawText(stack, new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_TOO_COMPLEX), 0xFFFFFF);
                break;
            case MULTIPLE_CONTROLLERS:
                drawText(stack, new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS), 0xFFFFFF);
                break;
            case READY:
                switch (tileEntity.getRunState()) {
                    case STOPPED:
                    case LOADING_DEVICES:
                        final ITextComponent bootError = tileEntity.getBootError();
                        if (bootError != null) {
                            drawText(stack, bootError, 0xFFFFFF);
                        }
                        break;
                }
                break;
        }

        stack.pop();
    }

    private void drawText(final MatrixStack stack, final ITextComponent text, final int color) {
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
            renderQuad(matrix, TEXTURE_STATUS.getBuffer(buffer, OpenComputersRenderType::getUnlitBlock));
        }
    }

    private void renderPower(final Matrix4f matrix, final IRenderTypeBuffer buffer) {
        renderQuad(matrix, TEXTURE_POWER.getBuffer(buffer, OpenComputersRenderType::getUnlitBlock));
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
