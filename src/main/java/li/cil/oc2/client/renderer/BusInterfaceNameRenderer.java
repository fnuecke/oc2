package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public enum BusInterfaceNameRenderer {
    INSTANCE;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void handleRenderLastEvent(final RenderWorldLastEvent event) {
        final Minecraft mc = Minecraft.getInstance();
        final PlayerEntity player = mc.player;
        final World level = player.level;

        if (!Wrenches.isHoldingWrench(player)) {
            return;
        }

        if (!(mc.hitResult instanceof BlockRayTraceResult)) {
            return;
        }

        final BlockRayTraceResult hit = (BlockRayTraceResult) mc.hitResult;
        final BlockPos blockPos = hit.getBlockPos();
        final TileEntity tileEntity = level.getBlockEntity(blockPos);
        if (!(tileEntity instanceof BusCableTileEntity)) {
            return;
        }

        final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
        final Direction side = BusCableBlock.getHitSide(blockPos, hit);
        if (BusCableBlock.getConnectionType(level.getBlockState(blockPos), side) != BusCableBlock.ConnectionType.INTERFACE) {
            return;
        }

        final String name = busCable.getInterfaceName(side);
        if (name.isEmpty()) {
            return;
        }


        final MatrixStack stack = event.getMatrixStack();
        stack.pushPose();

        stack.translate(0.5, 1, 0.5);
        stack.translate(side.getStepX() * 0.5f, 0, side.getStepZ() * 0.5f);

        final ActiveRenderInfo info = mc.gameRenderer.getMainCamera();
        stack.translate(
                blockPos.getX() - info.getPosition().x,
                blockPos.getY() - info.getPosition().y,
                blockPos.getZ() - info.getPosition().z);

        final EntityRendererManager renderManager = mc.getEntityRenderDispatcher();
        stack.mulPose(renderManager.cameraOrientation());

        stack.scale(-0.025f, -0.025f, 0.025f);

        final Matrix4f matrix = stack.last().pose();

        final FontRenderer fontrenderer = renderManager.getFont();
        final IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());

        final float horizontalTextOffset = -fontrenderer.width(name) * 0.5f;
        final float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        final int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;
        final int packedLight = LightTexture.pack(15, 15);

        fontrenderer.drawInBatch(name, horizontalTextOffset, 0, 0xffffffff,
                false, matrix, buffer, true, backgroundColor, packedLight);
        fontrenderer.drawInBatch(name, horizontalTextOffset, 0, 0xffffffff,
                false, matrix, buffer, false, 0, packedLight);

        buffer.endBatch();

        stack.popPose();
    }
}
