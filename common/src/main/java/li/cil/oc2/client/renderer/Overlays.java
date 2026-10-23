/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

public final class Overlays {
    private static final int PULSE_TICKS = 40;

    private static final Identifier POWER_LOCATION = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/overlay/power");
    private static final Identifier STATUS_LOCATION = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/overlay/status");
    private static final Identifier TERMINAL_LOCATION = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/overlay/terminal");

    private static final Material POWER = new Material(TextureAtlas.LOCATION_BLOCKS, POWER_LOCATION);
    private static final Material STATUS = new Material(TextureAtlas.LOCATION_BLOCKS, STATUS_LOCATION);
    private static final Material TERMINAL = new Material(TextureAtlas.LOCATION_BLOCKS, TERMINAL_LOCATION);

    private static final RenderType UNLIT_BLOCKS = ModRenderType.getUnlitBlock(TextureAtlas.LOCATION_BLOCKS);
    private static final SubmitNodeCollector.CustomGeometryRenderer POWER_QUAD = quad(POWER);
    private static final SubmitNodeCollector.CustomGeometryRenderer STATUS_QUAD = quad(STATUS);
    private static final SubmitNodeCollector.CustomGeometryRenderer TERMINAL_QUAD = quad(TERMINAL);

    // --------------------------------------------------------------------- //

    public static void renderPower(final PoseStack stack, final SubmitNodeCollector collector) {
        collector.submitCustomGeometry(stack, UNLIT_BLOCKS, POWER_QUAD);
    }

    public static void renderStatus(final PoseStack stack, final SubmitNodeCollector collector) {
        collector.submitCustomGeometry(stack, UNLIT_BLOCKS, STATUS_QUAD);
    }

    public static void renderStatus(final PoseStack stack, final SubmitNodeCollector collector, final int frequency) {
        if (System.currentTimeMillis() / frequency % 2 == 1) {
            renderStatus(stack, collector);
        }
    }

    public static void renderTerminal(final PoseStack stack, final SubmitNodeCollector collector) {
        collector.submitCustomGeometry(stack, UNLIT_BLOCKS, TERMINAL_QUAD);
    }

    public static void renderBox(final PoseStack stack, final SubmitNodeCollector collector, final AABB bounds, final Vector3f color, final Vector3f colorBright, final long gameTime, final float partialTicks) {
        renderBox(ModRenderType.getIndicator(), stack, collector, bounds, color, colorBright, gameTime, partialTicks);
    }

    public static void renderBox(final RenderType renderType, final PoseStack stack, final SubmitNodeCollector collector, final AABB bounds, final Vector3f color, final Vector3f colorBright, final long gameTime, final float partialTicks) {
        final float phase = (gameTime % PULSE_TICKS + partialTicks) / PULSE_TICKS;
        final float blend = (1 + Mth.sin(phase * (float) (Math.PI * 2))) * 0.5f;

        final float r = Mth.lerp(blend, color.x(), colorBright.x());
        final float g = Mth.lerp(blend, color.y(), colorBright.y());
        final float b = Mth.lerp(blend, color.z(), colorBright.z());

        collector.submitCustomGeometry(stack, renderType, (pose, consumer) -> addBoxVertices(pose, consumer, bounds, r, g, b));
    }

    // --------------------------------------------------------------------- //

    private static void addBoxVertices(final PoseStack.Pose matrix, final VertexConsumer consumer, final AABB bounds, final float r, final float g, final float b) {
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

    private static void addVertex(final PoseStack.Pose matrix, final VertexConsumer consumer, final float x, final float y, final float z, final float r, final float g, final float b) {
        consumer.addVertex(matrix, x, y, z).setColor(r, g, b, 1f);
    }

    private static SubmitNodeCollector.CustomGeometryRenderer quad(final Material material) {
        return (pose, consumer) -> {
            final TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(material);
            consumer.addVertex(pose, 0, 0, 0).setUv(sprite.getU0(), sprite.getV0());
            consumer.addVertex(pose, 0, 16, 0).setUv(sprite.getU0(), sprite.getV1());
            consumer.addVertex(pose, 16, 16, 0).setUv(sprite.getU1(), sprite.getV1());
            consumer.addVertex(pose, 16, 0, 0).setUv(sprite.getU1(), sprite.getV0());
        };
    }

    private Overlays() {
    }
}
