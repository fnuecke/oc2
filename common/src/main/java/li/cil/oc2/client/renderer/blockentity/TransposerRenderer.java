/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.blockentity.TransposerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public final class TransposerRenderer implements BlockEntityRenderer<TransposerBlockEntity> {
    private static final Material TOP = material("top");
    private static final Material SIDE = material("side");
    private static final Material BOTTOM = material("bottom");

    private static final float NEAR = 0.5f / 16f;
    private static final float FAR = 15.5f / 16f;

    // --------------------------------------------------------------------- //

    public TransposerRenderer(final BlockEntityRendererProvider.Context ignoredContext) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public void render(final TransposerBlockEntity transposer, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        final Matrix4f matrix = stack.last().pose();

        final VertexConsumer top = TOP.buffer(bufferSource, ModRenderType::getUnlitBlock);
        quad(matrix, top, 0, FAR, 0, 0, FAR, 1, 1, FAR, 1, 1, FAR, 0);

        final VertexConsumer bottom = BOTTOM.buffer(bufferSource, ModRenderType::getUnlitBlock);
        quad(matrix, bottom, 0, NEAR, 0, 0, NEAR, 1, 1, NEAR, 1, 1, NEAR, 0);

        final VertexConsumer side = SIDE.buffer(bufferSource, ModRenderType::getUnlitBlock);
        quad(matrix, side, 0, 1, NEAR, 0, 0, NEAR, 1, 0, NEAR, 1, 1, NEAR);
        quad(matrix, side, 0, 1, FAR, 0, 0, FAR, 1, 0, FAR, 1, 1, FAR);
        quad(matrix, side, NEAR, 1, 0, NEAR, 0, 0, NEAR, 0, 1, NEAR, 1, 1);
        quad(matrix, side, FAR, 1, 0, FAR, 0, 0, FAR, 0, 1, FAR, 1, 1);
    }

    // --------------------------------------------------------------------- //

    private static Material material(final String name) {
        return new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/transposer/" + name));
    }

    private static void quad(final Matrix4f matrix, final VertexConsumer consumer,
                             final float x0, final float y0, final float z0,
                             final float x1, final float y1, final float z1,
                             final float x2, final float y2, final float z2,
                             final float x3, final float y3, final float z3) {
        // Not chained: vanilla's SpriteCoordinateExpander.addVertex returns the delegate, so a
        // chained setUv would bypass the sprite remap (NeoForge patches this, Fabric does not).
        consumer.addVertex(matrix, x0, y0, z0);
        consumer.setUv(0, 0);

        consumer.addVertex(matrix, x1, y1, z1);
        consumer.setUv(0, 1);

        consumer.addVertex(matrix, x2, y2, z2);
        consumer.setUv(1, 1);

        consumer.addVertex(matrix, x3, y3, z3);
        consumer.setUv(1, 0);
    }
}
