package li.cil.oc2.client.renderer.tileentity;

import com.mojang.blaze3d.matrix.PoseStack;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.common.tileentity.NetworkConnectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.BlockEntityRenderer;
import net.minecraft.client.renderer.tileentity.BlockEntityRendererDispatcher;
import net.minecraft.util.math.BlockPos;

public final class NetworkConnectorBlockEntityRenderer extends BlockEntityRenderer<NetworkConnectorBlockEntity> {
    public NetworkConnectorBlockEntityRenderer(final BlockEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final NetworkConnectorBlockEntity connector, final float partialTicks, final PoseStack matrixStack, final IRenderTypeBuffer buffer, final int light, final int overlay) {
        // We do cable rendering as a fall-back in the TESR when Fabulous rendering is enabled.
        // We need to do this because there's no hook to render before the Fabulous full-screen
        // effects are rendered, which, sadly, completely ruin the depth buffer for us.
        if (!Minecraft.useShaderTransparency()) {
            return;
        }

        final BlockPos from = connector.getBlockPos();

        matrixStack.pushPose();
        matrixStack.translate(-from.getX(), -from.getY(), -from.getZ());

        NetworkCableRenderer.renderCablesFor(renderer.level, matrixStack, renderer.camera.getPosition(), connector);

        matrixStack.popPose();
    }
}
