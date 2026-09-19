/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public final class BlockOverlays {
    private static final ResourceLocation POWER_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/overlay/power");
    private static final ResourceLocation STATUS_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/overlay/status");
    private static final ResourceLocation TERMINAL_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/overlay/terminal");

    private static final Material POWER = new Material(InventoryMenu.BLOCK_ATLAS, POWER_LOCATION);
    private static final Material STATUS = new Material(InventoryMenu.BLOCK_ATLAS, STATUS_LOCATION);
    private static final Material TERMINAL = new Material(InventoryMenu.BLOCK_ATLAS, TERMINAL_LOCATION);

    // --------------------------------------------------------------------- //

    private BlockOverlays() {
    }

    // --------------------------------------------------------------------- //

    public static void renderPower(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        render(matrix, bufferSource, POWER);
    }

    public static void renderStatus(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        render(matrix, bufferSource, STATUS);
    }

    public static void renderStatus(final Matrix4f matrix, final MultiBufferSource bufferSource, final int frequency) {
        if (System.currentTimeMillis() / frequency % 2 == 1) {
            renderStatus(matrix, bufferSource);
        }
    }

    public static void renderTerminal(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        render(matrix, bufferSource, TERMINAL);
    }

    // --------------------------------------------------------------------- //

    private static void render(final Matrix4f matrix, final MultiBufferSource bufferSource, final Material material) {
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
}
