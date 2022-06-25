/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.integration.Wrenches;
import net.minecraft.client.Camera;
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
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public enum BusInterfaceNameRenderer {
    INSTANCE;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void handleRenderLastEvent(final RenderLevelLastEvent event) {
        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;
        if (player == null) {
            return;
        }

        final Level level = player.level;

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

        final PoseStack stack = event.getPoseStack();
        stack.pushPose();

        stack.translate(0.5, 1, 0.5);
        stack.translate(side.getStepX() * 0.5f, 0, side.getStepZ() * 0.5f);

        final Camera info = mc.gameRenderer.getMainCamera();
        stack.translate(
            blockPos.getX() - info.getPosition().x,
            blockPos.getY() - info.getPosition().y,
            blockPos.getZ() - info.getPosition().z);

        final EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
        stack.mulPose(renderManager.cameraOrientation());

        stack.scale(-0.025f, -0.025f, 0.025f);

        final Matrix4f matrix = stack.last().pose();

        final Font font = Minecraft.getInstance().font;
        final MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        final float horizontalTextOffset = -font.width(name) * 0.5f;
        final float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        final int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;
        final int packedLight = LightTexture.pack(15, 15);

        font.drawInBatch(name, horizontalTextOffset, 0, 0xffffffff,
            false, matrix, buffer, true, backgroundColor, packedLight);
        font.drawInBatch(name, horizontalTextOffset, 0, 0xffffffff,
            false, matrix, buffer, false, 0, packedLight);

        buffer.endBatch();

        stack.popPose();
    }
}
