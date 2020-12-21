package li.cil.oc2.client.render.tile;

import com.mojang.blaze3d.platform.GlStateManager;
import li.cil.oc2.api.API;
import li.cil.oc2.client.render.OpenComputersRenderType;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.vm.Terminal;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

public final class ComputerTileEntityRenderer extends BlockEntityRenderer<ComputerTileEntity> {
    private static final Identifier OVERLAY_POWER_LOCATION = new Identifier(API.MOD_ID, "blocks/computer/computer_overlay_power");
    private static final Identifier OVERLAY_STATUS_LOCATION = new Identifier(API.MOD_ID, "blocks/computer/computer_overlay_status");
    private static final Identifier OVERLAY_TERMINAL_LOCATION = new Identifier(API.MOD_ID, "blocks/computer/computer_overlay_terminal");

    private static final SpriteIdentifier TEXTURE_POWER = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, OVERLAY_POWER_LOCATION);
    private static final SpriteIdentifier TEXTURE_STATUS = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, OVERLAY_STATUS_LOCATION);
    private static final SpriteIdentifier TEXTURE_TERMINAL = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, OVERLAY_TERMINAL_LOCATION);

    ///////////////////////////////////////////////////////////////////

    public static void registerComputerAtlasTextures(final SpriteAtlasTexture atlas, final ClientSpriteRegistryCallback.Registry registry) {
        registry.register(OVERLAY_POWER_LOCATION);
        registry.register(OVERLAY_STATUS_LOCATION);
        registry.register(OVERLAY_TERMINAL_LOCATION);
    }

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntityRenderer(final BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(final ComputerTileEntity tileEntity, final float partialTicks, final MatrixStack stack, final VertexConsumerProvider buffer, final int combinedLightIn, final int combinedOverlayIn) {
        final Direction blockFacing = tileEntity.getCachedState().get(ComputerBlock.FACING);
        final Vec3d cameraPosition = dispatcher.camera.getFocusedEntity().getCameraPosVec(partialTicks);

        // If viewer is not in front of the block we can skip all of the rest, it cannot be visible.
        // We check against the center of the block instead of the actual relevant face for simplicity.
        final Vec3d relativeCameraPosition = cameraPosition.subtract(Vec3d.ofCenter(tileEntity.getPos()));
        final double projectedCameraPosition = relativeCameraPosition.dotProduct(Vec3d.of(blockFacing.getVector()));
        if (projectedCameraPosition <= 0) {
            return;
        }

        stack.push();

        // Align with front face of block.
        final Quaternion rotation = new Quaternion(Vector3f.NEGATIVE_Y, blockFacing.asRotation() + 180, true);
        stack.translate(0.5f, 0, 0.5f);
        stack.multiply(rotation);
        stack.translate(-0.5f, 0, -0.5f);

        // Flip and align with top left corner.
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        if (tileEntity.isRunning()) {
            renderTerminal(tileEntity, stack, buffer, cameraPosition);
        }

        stack.translate(0, 0, -0.1f);
        final Matrix4f matrix = stack.peek().getModel();

        switch (tileEntity.getBusState()) {
            case SCAN_PENDING:
            case INCOMPLETE:
                drawStatus(matrix, buffer);
                break;
            case TOO_COMPLEX:
                drawStatus(matrix, buffer, 1000);
                break;
            case MULTIPLE_CONTROLLERS:
                drawStatus(matrix, buffer, 250);
                break;
            case READY:
                switch (tileEntity.getRunState()) {
                    case STOPPED:
                        break;
                    case LOADING_DEVICES:
                        drawStatus(matrix, buffer);
                        break;
                    case RUNNING:
                        drawPower(matrix, buffer);
                        break;
                }
                break;
        }

        stack.pop();
    }

    ///////////////////////////////////////////////////////////////////

    private void renderTerminal(final ComputerTileEntity tileEntity, final MatrixStack stack, final VertexConsumerProvider buffer, final Vec3d cameraPosition) {
        // Render terminal content if close enough.
        if (Vec3d.ofCenter(tileEntity.getPos()).isInRange(cameraPosition, 6f * 6f)) {
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

            final Matrix4f matrix = stack.peek().getModel();
            drawQuad(matrix, TEXTURE_TERMINAL.getVertexConsumer(buffer, OpenComputersRenderType::getUnlitBlock));

            stack.pop();
        }
    }

    private void drawStatus(final Matrix4f matrix, final VertexConsumerProvider buffer) {
        drawStatus(matrix, buffer, 0);
    }

    private void drawStatus(final Matrix4f matrix, final VertexConsumerProvider buffer, final int frequency) {
        if (frequency <= 0 || (((System.currentTimeMillis() + hashCode()) / frequency) % 2) == 1) {
            drawQuad(matrix, TEXTURE_STATUS.getVertexConsumer(buffer, OpenComputersRenderType::getUnlitBlock));
        }
    }

    private void drawPower(final Matrix4f matrix, final VertexConsumerProvider buffer) {
        drawQuad(matrix, TEXTURE_POWER.getVertexConsumer(buffer, OpenComputersRenderType::getUnlitBlock));
    }

    private static void drawQuad(final Matrix4f matrix, final VertexConsumer buffer) {
        // NB: We may get a SpriteAwareVertexBuilder here. Sadly, its chaining is broken,
        //     because methods may return the underlying vertex builder, so e.g. calling
        //     buffer.pos(...).tex(...) will not actually call SpriteAwareVertexBuilder.tex(...)
        //     but SpriteAwareVertexBuilder.vertexBuilder.tex(...), skipping the UV remapping.
        buffer.vertex(matrix, 0, 0, 0);
        buffer.texture(0, 0);
        buffer.next();
        buffer.vertex(matrix, 0, 16, 0);
        buffer.texture(0, 1);
        buffer.next();
        buffer.vertex(matrix, 16, 16, 0);
        buffer.texture(1, 1);
        buffer.next();
        buffer.vertex(matrix, 16, 0, 0);
        buffer.texture(1, 0);
        buffer.next();
    }
}
