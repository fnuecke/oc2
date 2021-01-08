package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import li.cil.oc2.common.tileentity.NetworkConnectorTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public final class NetworkCableRenderer {
    private static final int MAX_RENDER_DISTANCE = 100;
    private static final int CABLE_VERTEX_COUNT = 9;
    private static final float CABLE_THICKNESS = 0.025f;
    private static final float CABLE_LENGTH_FOR_MAX_SWING = 6f;
    private static final float CABLE_MAX_SWING_AMOUNT = 0.05f;
    private static final int CABLE_SWING_INTERVAL = 8000;
    private static final float CABLE_HANG_MIN = 0.1f;
    private static final float CABLE_HANG_MAX = 0.5f;
    private static final float CABLE_MAX_LENGTH = 8f;
    private static final Vector3f CABLE_COLOR = new Vector3f(0.0f, 0.33f, 0.4f);

    private static final ArrayList<NetworkConnectorTileEntity> connectors = new ArrayList<>();
    private static final ArrayList<Connection> connections = new ArrayList<>();
    private static boolean isDirty;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(NetworkCableRenderer::handleRenderWorld);
    }

    public static void addNetworkConnector(final NetworkConnectorTileEntity connector) {
        connectors.add(connector);
        invalidateConnections();
    }

    public static void invalidateConnections() {
        isDirty = true;
    }

    @SubscribeEvent
    public static void handleRenderWorld(final RenderWorldLastEvent event) {
        validateConnectors();
        validatePairs();

        if (connections.isEmpty()) {
            return;
        }

        final World world = Minecraft.getInstance().world;
        if (world == null) {
            return;
        }

        final MatrixStack matrixStack = event.getMatrixStack();

        final ActiveRenderInfo activeRenderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        final Vector3d eye = activeRenderInfo.getProjectedView();

        final ClippingHelper frustum = new ClippingHelper(matrixStack.getLast().getMatrix(), event.getProjectionMatrix());
        frustum.setCameraPosition(eye.getX(), eye.getY(), eye.getZ());

        matrixStack.push();
        matrixStack.translate(-eye.getX(), -eye.getY(), -eye.getZ());

        final Matrix4f viewMatrix = matrixStack.getLast().getMatrix();

        final RenderType renderType = OpenComputersRenderType.getNetworkCable();
        final IRenderTypeBuffer.Impl bufferSource = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();

        final float r = CABLE_COLOR.getX();
        final float g = CABLE_COLOR.getY();
        final float b = CABLE_COLOR.getZ();

        for (final Connection connection : connections) {
            final Vector3d p0 = connection.from;
            final Vector3d p1 = connection.to;

            if (!p0.isWithinDistanceOf(eye, MAX_RENDER_DISTANCE) && !p1.isWithinDistanceOf(eye, MAX_RENDER_DISTANCE)) {
                continue;
            }

            // We may easily get false positives here for diagonal cables, but it's good enough for now.
            if (!frustum.isBoundingBoxInFrustum(connection.bounds)) {
                continue;
            }

            final Vector3d p2 = animateCableSwing(
                    lerp(p0, p1, 0.5f).subtract(0, computeCableHang(p0, p1), 0),
                    connection.right,
                    computeCableSwingAmount(p0, p1),
                    connection.hashCode());

            final IVertexBuilder buffer = bufferSource.getBuffer(renderType);

            for (int i = 0; i < CABLE_VERTEX_COUNT; i++) {
                final float t = i / (CABLE_VERTEX_COUNT - 1f);
                final Vector3d p = quadraticBezier(p0, p1, p2, t);
                final Vector3d n = getExtrusionVector(eye, p, connection.forward);

                final BlockPos blockPos = new BlockPos(p);
                final int blockLight = world.getLightFor(LightType.BLOCK, blockPos);
                final int skyLight = world.getLightFor(LightType.SKY, blockPos);
                final int packedLight = LightTexture.packLight(blockLight, skyLight);

                final Vector3f v0 = new Vector3f(p.subtract(n));
                final Vector3f v1 = new Vector3f(p.add(n));

                buffer.pos(viewMatrix, v0.getX(), v0.getY(), v0.getZ())
                        .color(r, g, b, 1f)
                        .lightmap(packedLight)
                        .endVertex();
                buffer.pos(viewMatrix, v1.getX(), v1.getY(), v1.getZ())
                        .color(r, g, b, 1f)
                        .lightmap(packedLight)
                        .endVertex();
            }

            bufferSource.finish(renderType);
        }

        matrixStack.pop();
    }

    ///////////////////////////////////////////////////////////////////

    private static Vector3d lerp(final Vector3d a, final Vector3d b, final float t) {
        return a.add(b.subtract(a).scale(t)); // a + (b - a)*t = a*(1-t) + b*t
    }

    private static Vector3d quadraticBezier(final Vector3d a, final Vector3d b, final Vector3d c, final float t) {
        final Vector3d a1 = lerp(a, c, t);
        final Vector3d b1 = lerp(c, b, t);
        return lerp(a1, b1, t);
    }

    private static Vector3d getExtrusionVector(final Vector3d eye, final Vector3d v, final Vector3d forward) {
        return forward.crossProduct(eye.subtract(v)).normalize().scale(CABLE_THICKNESS);
    }

    private static float computeCableHang(final Vector3d a, final Vector3d b) {
        final double length = a.distanceTo(b);
        final double hangFactor = MathHelper.clamp(length / CABLE_MAX_LENGTH, 0, 1);
        return (float) (CABLE_HANG_MIN + (CABLE_HANG_MAX - CABLE_HANG_MIN) * hangFactor);
    }

    private static float computeCableSwingAmount(final Vector3d p0, final Vector3d p1) {
        return MathHelper.clamp((float) p0.distanceTo(p1) / CABLE_LENGTH_FOR_MAX_SWING, 0.1f, 1f) * CABLE_MAX_SWING_AMOUNT;
    }

    private static Vector3d animateCableSwing(final Vector3d c, @Nullable final Vector3d right, final float swingAmount, final int seed) {
        final float relTime = ((System.currentTimeMillis() + seed) % CABLE_SWING_INTERVAL) / (float) CABLE_SWING_INTERVAL;
        final float relRadialTime = relTime * 2 * (float) Math.PI;

        if (right == null) {
            return c.add(swingAmount * MathHelper.sin(relRadialTime),
                    0,
                    swingAmount * MathHelper.cos(relRadialTime));
        } else {
            return c.add(swingAmount * MathHelper.cos(relRadialTime) * right.getX(),
                    0.5f * swingAmount * MathHelper.sin(relRadialTime * 2 - (float) Math.PI) - swingAmount,
                    swingAmount * MathHelper.cos(relRadialTime) * right.getZ());
        }
    }

    private static void validateConnectors() {
        for (int i = connectors.size() - 1; i >= 0; i--) {
            final NetworkConnectorTileEntity connector = connectors.get(i);
            if (connector.isRemoved()) {
                connectors.remove(i);
                invalidateConnections();
            }
        }
    }

    private static void validatePairs() {
        if (!isDirty) {
            return;
        }

        isDirty = false;
        connections.clear();

        final HashSet<Connection> seen = new HashSet<>();
        for (final NetworkConnectorTileEntity connector : connectors) {
            final BlockPos position = connector.getPos();
            for (final BlockPos connectedPosition : connector.getConnectedPositions()) {
                final Connection connection = new Connection(position, connectedPosition);
                if (seen.add(connection)) {
                    connections.add(connection);
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class Connection {
        private static final Vector3d POS_Y = new Vector3d(0, 1, 0);

        public final BlockPos fromPos, toPos;
        public final Vector3d from, to, forward, right;
        public final AxisAlignedBB bounds;

        private Connection(final BlockPos fromPos, final BlockPos toPos) {
            if (fromPos.compareTo(toPos) > 0) {
                this.fromPos = toPos;
                this.toPos = fromPos;
            } else {
                this.fromPos = fromPos;
                this.toPos = toPos;
            }

            from = Vector3d.copyCentered(fromPos);
            to = Vector3d.copyCentered(toPos);
            forward = to.subtract(from).normalize();
            right = fromPos.getX() == toPos.getX() && fromPos.getZ() == toPos.getZ()
                    ? null : forward.crossProduct(POS_Y);
            bounds = new AxisAlignedBB(from, to).grow(0, CABLE_HANG_MAX, 0);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Connection that = (Connection) o;
            return fromPos.equals(that.fromPos) && toPos.equals(that.toPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromPos, toPos);
        }
    }
}
