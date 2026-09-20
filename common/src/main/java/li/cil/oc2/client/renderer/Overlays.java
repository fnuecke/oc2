/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Overlays {
    private static final int PULSE_TICKS = 40;

    private static final ResourceLocation POWER_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/overlay/power");
    private static final ResourceLocation STATUS_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/overlay/status");
    private static final ResourceLocation TERMINAL_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/overlay/terminal");

    private static final Material POWER = new Material(InventoryMenu.BLOCK_ATLAS, POWER_LOCATION);
    private static final Material STATUS = new Material(InventoryMenu.BLOCK_ATLAS, STATUS_LOCATION);
    private static final Material TERMINAL = new Material(InventoryMenu.BLOCK_ATLAS, TERMINAL_LOCATION);

    // --------------------------------------------------------------------- //

    public static void renderPower(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderQuad(matrix, bufferSource, POWER);
    }

    public static void renderStatus(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderQuad(matrix, bufferSource, STATUS);
    }

    public static void renderStatus(final Matrix4f matrix, final MultiBufferSource bufferSource, final int frequency) {
        if (System.currentTimeMillis() / frequency % 2 == 1) {
            renderStatus(matrix, bufferSource);
        }
    }

    public static void renderTerminal(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderQuad(matrix, bufferSource, TERMINAL);
    }

    public static void renderBox(final PoseStack stack, final MultiBufferSource bufferSource, final AABB bounds, final Vector3f color, final Vector3f colorBright, final long gameTime, final float partialTicks) {
        renderBox(ModRenderType.getIndicator(), stack, bufferSource, bounds, color, colorBright, gameTime, partialTicks);
    }

    public static void renderBox(final RenderType renderType, final PoseStack stack, final MultiBufferSource bufferSource, final AABB bounds, final Vector3f color, final Vector3f colorBright, final long gameTime, final float partialTicks) {
        final float phase = (gameTime % PULSE_TICKS + partialTicks) / PULSE_TICKS;
        final float blend = (1 + Mth.sin(phase * (float) (Math.PI * 2))) * 0.5f;

        final float r = Mth.lerp(blend, color.x(), colorBright.x());
        final float g = Mth.lerp(blend, color.y(), colorBright.y());
        final float b = Mth.lerp(blend, color.z(), colorBright.z());

        addBoxVertices(stack.last().pose(), bufferSource.getBuffer(renderType), bounds, r, g, b);
    }

    // --------------------------------------------------------------------- //

    private static void addBoxVertices(final Matrix4f matrix, final VertexConsumer consumer, final AABB bounds, final float r, final float g, final float b) {
        final float x0 = (float) bounds.minX;
        final float y0 = (float) bounds.minY;
        final float z0 = (float) bounds.minZ;
        final float x1 = (float) bounds.maxX;
        final float y1 = (float) bounds.maxY;
        final float z1 = (float) bounds.maxZ;

        addVertex(matrix, consumer, x0, y0, z0, r, g, b);
        addVertex(matrix, consumer, x0, y1, z0, r, g, b);
        addVertex(matrix, consumer, x1, y1, z0, r, g, b);
        addVertex(matrix, consumer, x1, y0, z0, r, g, b);

        addVertex(matrix, consumer, x0, y0, z1, r, g, b);
        addVertex(matrix, consumer, x1, y0, z1, r, g, b);
        addVertex(matrix, consumer, x1, y1, z1, r, g, b);
        addVertex(matrix, consumer, x0, y1, z1, r, g, b);

        addVertex(matrix, consumer, x0, y0, z0, r, g, b);
        addVertex(matrix, consumer, x0, y0, z1, r, g, b);
        addVertex(matrix, consumer, x0, y1, z1, r, g, b);
        addVertex(matrix, consumer, x0, y1, z0, r, g, b);

        addVertex(matrix, consumer, x1, y0, z0, r, g, b);
        addVertex(matrix, consumer, x1, y1, z0, r, g, b);
        addVertex(matrix, consumer, x1, y1, z1, r, g, b);
        addVertex(matrix, consumer, x1, y0, z1, r, g, b);

        addVertex(matrix, consumer, x0, y0, z0, r, g, b);
        addVertex(matrix, consumer, x1, y0, z0, r, g, b);
        addVertex(matrix, consumer, x1, y0, z1, r, g, b);
        addVertex(matrix, consumer, x0, y0, z1, r, g, b);

        addVertex(matrix, consumer, x0, y1, z0, r, g, b);
        addVertex(matrix, consumer, x0, y1, z1, r, g, b);
        addVertex(matrix, consumer, x1, y1, z1, r, g, b);
        addVertex(matrix, consumer, x1, y1, z0, r, g, b);
    }

    private static void addVertex(final Matrix4f matrix, final VertexConsumer consumer, final float x, final float y, final float z, final float r, final float g, final float b) {
        consumer.addVertex(matrix, x, y, z).setColor(r, g, b, 1f);
    }

    private static void renderQuad(final Matrix4f matrix, final MultiBufferSource bufferSource, final Material material) {
        final VertexConsumer consumer = material.buffer(bufferSource, ModRenderType::getUnlitBlock);

        // Not chained: vanilla's SpriteCoordinateExpander.addVertex returns the delegate, so a
        // chained setUv would bypass the sprite remap (NeoForge patches this, Fabric does not).
        consumer.addVertex(matrix, 0, 0, 0);
        consumer.setUv(0, 0);

        consumer.addVertex(matrix, 0, 16, 0);
        consumer.setUv(0, 1);

        consumer.addVertex(matrix, 16, 16, 0);
        consumer.setUv(1, 1);

        consumer.addVertex(matrix, 16, 0, 0);
        consumer.setUv(1, 0);
    }

    private Overlays() {
    }
}
