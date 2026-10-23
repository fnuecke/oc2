/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.common.block.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public final class NetworkConnectorRenderer implements BlockEntityRenderer<NetworkConnectorBlockEntity, NetworkConnectorRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private boolean hasAdjacentInterface;
        private long gameTime;
        private float partialTicks;
    }

    // --------------------------------------------------------------------- //

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
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final NetworkConnectorBlockEntity connector, final State state, final float partialTicks, final Vec3 cameraPosition,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(connector, state, partialTicks, cameraPosition, crumbling);

        final Level level = connector.getLevel();
        state.hasAdjacentInterface = connector.hasAdjacentInterface();
        state.gameTime = level != null ? level.getGameTime() : 0;
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        if (!state.hasAdjacentInterface) {
            return;
        }

        final AABB bounds = INDICATOR_BOUNDS[NetworkConnectorBlock.getFacing(state.blockState).ordinal()];

        Overlays.renderBox(stack, collector, bounds, INDICATOR_COLOR, INDICATOR_COLOR_BRIGHT, state.gameTime, state.partialTicks);
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
