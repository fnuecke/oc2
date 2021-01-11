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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

// Note on cable rendering and Fabulous rendering mode: sadly at the time of writing,
// there is no hook to render before the Fabulous shaders. And those sadly appear to
// completely flatten / break the depth buffer. So using the RenderWorldLastEvent will
// not work anymore for rendering cables in one nice efficient batch. So instead we
// fall back to letting the TESRs trigger the cable rendering. We still use the data
// structures with precomputed data and such, it's just that they need much larger
// render bounds and require an addition hash map look-up.
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

    private static final Set<NetworkConnectorTileEntity> connectors = Collections.newSetFromMap(new WeakHashMap<>());
    private static int lastKnownConnectorCount;
    private static boolean isDirty;

    private static final ArrayList<Connection> connections = new ArrayList<>();
    private static final WeakHashMap<NetworkConnectorTileEntity, ArrayList<Connection>> connectionsByConnector = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(NetworkCableRenderer::handleRenderWorld);
        MinecraftForge.EVENT_BUS.addListener(NetworkCableRenderer::handleChunkUnloadEvent);
        MinecraftForge.EVENT_BUS.addListener(NetworkCableRenderer::handleWorldUnloadEvent);
    }

    public static void addNetworkConnector(final NetworkConnectorTileEntity connector) {
        connectors.add(connector);
        invalidateConnections();
    }

    public static void invalidateConnections() {
        isDirty = true;
    }

    public static void renderCablesFor(final IBlockDisplayReader world, final MatrixStack matrixStack, final Vector3d eye, final NetworkConnectorTileEntity connector) {
        final ArrayList<Connection> connections = connectionsByConnector.get(connector);
        if (connections != null) {
            renderCables(world, matrixStack, eye, connections, unused -> true);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static void handleChunkUnloadEvent(final ChunkEvent.Unload event) {
        if (event.getWorld().isRemote()) {
            final ChunkPos chunkPos = event.getChunk().getPos();

            final ArrayList<NetworkConnectorTileEntity> list = new ArrayList<>(NetworkCableRenderer.connectors);
            for (final NetworkConnectorTileEntity connector : list) {
                final ChunkPos connectorChunkPos = new ChunkPos(connector.getPos());
                if (Objects.equals(connectorChunkPos, chunkPos)) {
                    connectors.remove(connector);
                }
            }

            invalidateConnections();
        }
    }

    private static void handleWorldUnloadEvent(final WorldEvent.Unload event) {
        if (event.getWorld().isRemote()) {
            final IWorld world = event.getWorld();

            final ArrayList<NetworkConnectorTileEntity> list = new ArrayList<>(NetworkCableRenderer.connectors);
            for (final NetworkConnectorTileEntity connector : list) {
                if (connector.getWorld() == world) {
                    connectors.remove(connector);
                }
            }

            invalidateConnections();
        }
    }

    private static void handleRenderWorld(final RenderWorldLastEvent event) {
        validateConnectors();
        validatePairs();

        if (Minecraft.isFabulousGraphicsEnabled()) {
            return;
        }

        if (connections.isEmpty()) {
            return;
        }

        final Minecraft client = Minecraft.getInstance();
        final World world = client.world;
        if (world == null) {
            return;
        }

        final MatrixStack matrixStack = event.getMatrixStack();

        final ActiveRenderInfo activeRenderInfo = client.gameRenderer.getActiveRenderInfo();
        final Vector3d eye = activeRenderInfo.getProjectedView();

        final ClippingHelper frustum = new ClippingHelper(matrixStack.getLast().getMatrix(), event.getProjectionMatrix());
        frustum.setCameraPosition(eye.getX(), eye.getY(), eye.getZ());

        matrixStack.push();
        matrixStack.translate(-eye.getX(), -eye.getY(), -eye.getZ());

        renderCables(world, matrixStack, eye, connections, frustum::isBoundingBoxInFrustum);

        matrixStack.pop();
    }

    private static void renderCables(final IBlockDisplayReader world, final MatrixStack matrixStack, final Vector3d eye, final ArrayList<Connection> connections, final Predicate<AxisAlignedBB> filter) {
        final Matrix4f viewMatrix = matrixStack.getLast().getMatrix();

        final RenderType renderType = CustomRenderType.getNetworkCable();
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
            if (!filter.test(connection.bounds)) {
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
    }

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
        final ArrayList<NetworkConnectorTileEntity> list = new ArrayList<>(connectors);
        for (final NetworkConnectorTileEntity connector : list) {
            if (connector.isRemoved()) {
                connectors.remove(connector);
                connectionsByConnector.remove(connector);
                invalidateConnections();
            }
        }

        // We track the size because the WeakHasMap may expunge dead entries without
        // us knowing otherwise.
        if (list.size() != lastKnownConnectorCount) {
            invalidateConnections();
        }
        lastKnownConnectorCount = list.size();
    }

    private static void validatePairs() {
        if (!isDirty) {
            return;
        }

        isDirty = false;
        connections.clear();
        connectionsByConnector.clear();

        final HashSet<Connection> seen = new HashSet<>();
        for (final NetworkConnectorTileEntity connector : connectors) {
            final BlockPos position = connector.getPos();
            for (final BlockPos connectedPosition : connector.getConnectedPositions()) {
                final Connection connection = new Connection(position, connectedPosition);
                if (seen.add(connection)) {
                    connections.add(connection);
                    connectionsByConnector.computeIfAbsent(connector, unused -> new ArrayList<>()).add(connection);
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
