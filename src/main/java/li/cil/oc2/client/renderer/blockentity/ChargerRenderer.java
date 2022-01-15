package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.blockentity.ChargerBlockEntity;
import li.cil.oc2.common.util.ChainableVertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

public final class ChargerRenderer implements BlockEntityRenderer<ChargerBlockEntity> {
    public static final ResourceLocation EFFECT_LOCATION = new ResourceLocation(API.MOD_ID, "block/charger/effect");

    private static final Material TEXTURE_EFFECT = new Material(InventoryMenu.BLOCK_ATLAS, EFFECT_LOCATION);

    private static final int EFFECT_LAYERS = 3;
    private static final float EFFECT_HEIGHT = 0.5f;
    private static final float EFFECT_SPEED = 0.1f;
    private static final float EFFECT_SCALE_START = 0.6f;
    private static final float EFFECT_SCALE_END = 0.8f;

    ///////////////////////////////////////////////////////////////////

    private float offset = 0;

    ///////////////////////////////////////////////////////////////////

    public ChargerRenderer(final BlockEntityRendererProvider.Context ignoredContext) {
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final ChargerBlockEntity charger, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        offset = (offset + EFFECT_SPEED * partialTicks / 20f) % (float) (Math.PI * 2);

        stack.pushPose();
        stack.translate(0.5, 1.1, 0.5);

        final VertexConsumer consumer = TEXTURE_EFFECT.buffer(bufferSource, ModRenderType::getUnlitBlock);

        for (int i = 0; i < EFFECT_LAYERS; i++) {
            final float relativeY = (1 + Mth.sin(offset + ((float) Math.PI * 2f * i / EFFECT_LAYERS))) * 0.5f;
            final float y = relativeY * EFFECT_HEIGHT;
            final float scale = EFFECT_SCALE_START + relativeY * (EFFECT_SCALE_END - EFFECT_SCALE_START);

            stack.pushPose();
            stack.translate(0, y, 0);
            renderScaledQuad(stack, consumer, scale);
            stack.popPose();
        }

        stack.popPose();
    }

    private static void renderScaledQuad(final PoseStack stack, final VertexConsumer consumer, final float scale) {
        stack.pushPose();
        stack.scale(scale, scale, scale);
        renderQuad(stack.last().pose(), consumer);
        stack.popPose();
    }

    private static void renderQuad(final Matrix4f matrix, final VertexConsumer consumer) {
        final VertexConsumer wrapper = new ChainableVertexConsumer(consumer);
        wrapper.vertex(matrix, -0.5f, 0, -0.5f)
            .uv(0, 0)
            .endVertex();

        wrapper.vertex(matrix, -0.5f, 0, 0.5f)
            .uv(0, 1)
            .endVertex();

        wrapper.vertex(matrix, 0.5f, 0, 0.5f)
            .uv(1, 1)
            .endVertex();

        wrapper.vertex(matrix, 0.5f, 0, -0.5f)
            .uv(1, 0)
            .endVertex();
    }
}
