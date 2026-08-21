/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.integration.Wrenches;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public enum BusInterfaceNameRenderer {
    INSTANCE;

    // Values mirror EntityRenderer.renderNameTag
    private static final double LABEL_HOVER = 0.5;
    private static final int SEE_THROUGH_COLOR = 0x20FFFFFF;
    private static final int COLOR = 0xFFFFFFFF;

    // --------------------------------------------------------------------- //

    public void render(final PoseStack poseStack) {
        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;
        if (player == null) {
            return;
        }

        final Level level = player.level();

        if (!Wrenches.isHoldingWrench(player)) {
            return;
        }

        if (!(mc.hitResult instanceof final BlockHitResult hit)) {
            return;
        }

        final BlockPos blockPos = hit.getBlockPos();
        final BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof final BusCableBlockEntity busCable)) {
            return;
        }

        final Direction side = BusCableBlock.getHitSide(blockPos, hit);
        if (BusCableBlock.getConnectionType(level.getBlockState(blockPos), side) != BusCableBlock.ConnectionType.INTERFACE) {
            return;
        }

        final String name = busCable.getInterfaceName(side);
        if (name.isEmpty()) {
            return;
        }

        final PoseStack stack = poseStack;
        stack.pushPose();

        final Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        stack.translate(
            blockPos.getX() + 0.5 + side.getStepX() * 0.5 - camera.x,
            blockPos.getY() + 0.5 + side.getStepY() * 0.5 - camera.y
                + (side == Direction.DOWN ? -LABEL_HOVER : LABEL_HOVER),
            blockPos.getZ() + 0.5 + side.getStepZ() * 0.5 - camera.z);

        final EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
        stack.mulPose(renderManager.cameraOrientation());

        stack.scale(0.025f, -0.025f, 0.025f);

        final Matrix4f matrix = stack.last().pose();

        final Font font = mc.font;
        final MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        final float horizontalTextOffset = -font.width(name) * 0.5f;
        final float backgroundOpacity = mc.options.getBackgroundOpacity(0.25F);
        final int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;
        final int packedLight = LightTexture.pack(15, 15);

        font.drawInBatch(name, horizontalTextOffset, 0, SEE_THROUGH_COLOR,
            false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, backgroundColor, packedLight);
        font.drawInBatch(name, horizontalTextOffset, 0, COLOR,
            false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        buffer.endBatch();

        stack.popPose();
    }
}
