package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;

public final class NetworkConnectorRenderer implements BlockEntityRenderer<NetworkConnectorBlockEntity> {
    private final BlockEntityRenderDispatcher renderer;

    ///////////////////////////////////////////////////////////////////

    public NetworkConnectorRenderer(final BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockEntityRenderDispatcher();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final NetworkConnectorBlockEntity connector, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        // We do cable rendering as a fall-back in the TER when Fabulous rendering is enabled.
        // We need to do this because there's no hook to render before the Fabulous full-screen
        // effects are rendered, which, sadly, completely ruin the depth buffer for us.
        if (!Minecraft.useShaderTransparency()) {
            return;
        }

        final BlockPos from = connector.getBlockPos();

        stack.pushPose();
        stack.translate(-from.getX(), -from.getY(), -from.getZ());

        NetworkCableRenderer.renderCablesFor(renderer.level, stack, renderer.camera.getPosition(), connector);

        stack.popPose();
    }
}
