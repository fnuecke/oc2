/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.renderer.IndicatorRenderer;
import li.cil.oc2.common.block.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

public final class NetworkConnectorRenderer implements BlockEntityRenderer<NetworkConnectorBlockEntity> {
    private static final Vector3f INDICATOR_COLOR = new Vector3f(0.35f, 0.95f, 1f);
    private static final Vector3f INDICATOR_COLOR_BRIGHT = new Vector3f(0.6f, 1f, 1f);

    private static final double CAP_SIDE_MIN = 6 / 16.0;
    private static final double CAP_SIDE_MAX = 10 / 16.0;
    private static final double CAP_AXIS_MIN = 7 / 16.0;
    private static final double CAP_AXIS_MAX = 8 / 16.0;

    private static final AABB[] INDICATOR_BOUNDS = createIndicatorBounds();

    // --------------------------------------------------------------------- //

    public NetworkConnectorRenderer(final BlockEntityRendererProvider.Context ignoredContext) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public void render(final NetworkConnectorBlockEntity connector, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        if (!connector.hasAdjacentInterface()) {
            return;
        }

        final Level level = connector.getLevel();
        final long gameTime = level != null ? level.getGameTime() : 0;
        final AABB bounds = INDICATOR_BOUNDS[NetworkConnectorBlock.getFacing(connector.getBlockState()).ordinal()];

        IndicatorRenderer.render(stack, bufferSource, bounds, INDICATOR_COLOR, INDICATOR_COLOR_BRIGHT, gameTime, partialTicks);
    }

    // --------------------------------------------------------------------- //

    private static AABB[] createIndicatorBounds() {
        final AABB[] bounds = new AABB[Direction.values().length];

        for (final Direction direction : Direction.values()) {
            final boolean isPositive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            final double min = isPositive ? CAP_AXIS_MIN : 1 - CAP_AXIS_MAX;
            final double max = isPositive ? CAP_AXIS_MAX : 1 - CAP_AXIS_MIN;

            bounds[direction.ordinal()] = switch (direction.getAxis()) {
                case X -> new AABB(min, CAP_SIDE_MIN, CAP_SIDE_MIN, max, CAP_SIDE_MAX, CAP_SIDE_MAX);
                case Y -> new AABB(CAP_SIDE_MIN, min, CAP_SIDE_MIN, CAP_SIDE_MAX, max, CAP_SIDE_MAX);
                case Z -> new AABB(CAP_SIDE_MIN, CAP_SIDE_MIN, min, CAP_SIDE_MAX, CAP_SIDE_MAX, max);
            };
        }

        return bounds;
    }
}
