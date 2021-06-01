package li.cil.oc2.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.CustomRenderType;
import li.cil.oc2.common.tileentity.ChargerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

public final class ChargerBlockEntityRenderer extends BlockEntityRenderer<ChargerBlockEntity> {
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

    public ChargerBlockEntityRenderer(final BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final ChargerBlockEntity tileEntity, final float partialTicks, final PoseStack matrixStack, final MultiBufferSource buffer, final int light, final int overlay) {
        offset = (offset + EFFECT_SPEED * partialTicks / 20f) % (float) (Math.PI * 2);

        matrixStack.pushPose();
        matrixStack.translate(0.5, 1.1, 0.5);

        final VertexConsumer builder = TEXTURE_EFFECT.buffer(buffer, CustomRenderType::getUnlitBlock);

        for (int i = 0; i < EFFECT_LAYERS; i++) {
            final float relativeY = (1 + Mth.sin(offset + ((float) Math.PI * 2f * i / EFFECT_LAYERS))) * 0.5f;
            final float y = relativeY * EFFECT_HEIGHT;
            final float scale = EFFECT_SCALE_START + relativeY * (EFFECT_SCALE_END - EFFECT_SCALE_START);

            matrixStack.pushPose();
            matrixStack.translate(0, y, 0);
            renderScaledQuad(matrixStack, builder, scale);
            matrixStack.popPose();
        }

        matrixStack.popPose();
    }

    private static void renderScaledQuad(final PoseStack matrixStack, final VertexConsumer builder, final float scale) {
        matrixStack.pushPose();
        matrixStack.scale(scale, scale, scale);
        renderQuad(matrixStack.last().pose(), builder);
        matrixStack.popPose();
    }

    private static void renderQuad(final Matrix4f matrix, final VertexConsumer builder) {
        // NB: We may get a SpriteAwareVertexBuilder here. Sadly, its chaining is broken,
        //     because methods may return the underlying vertex builder, so e.g. calling
        //     buffer.pos(...).tex(...) will not actually call SpriteAwareVertexBuilder.tex(...)
        //     but SpriteAwareVertexBuilder.vertexBuilder.tex(...), skipping the UV remapping.
        builder.vertex(matrix, -0.5f, 0, -0.5f);
        builder.uv(0, 0);
        builder.endVertex();

        builder.vertex(matrix, -0.5f, 0, 0.5f);
        builder.uv(0, 1);
        builder.endVertex();

        builder.vertex(matrix, 0.5f, 0, 0.5f);
        builder.uv(1, 1);
        builder.endVertex();

        builder.vertex(matrix, 0.5f, 0, -0.5f);
        builder.uv(1, 0);
        builder.endVertex();
    }
}
