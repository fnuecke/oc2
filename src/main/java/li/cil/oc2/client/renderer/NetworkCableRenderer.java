package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.common.tileentity.NetworkConnectorTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.Mth;
import com.mojang.math.Matrix4f;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Vector3f;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderLevelLastEvent;
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
    private static final ArrayList<CablePoint> cablePoints = new ArrayList<>();

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

    public static void renderCablesFor(final BlockAndTintGetter world, final PoseStack matrixStack, final Vec3 eye, final NetworkConnectorTileEntity connector) {
        final ArrayList<Connection> connections = connectionsByConnector.get(connector);
        if (connections != null) {
            renderCables(world, matrixStack, eye, connections, unused -> true);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static void handleChunkUnloadEvent(final ChunkEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            final ChunkPos chunkPos = event.getChunk().getPos();

            final ArrayList<NetworkConnectorTileEntity> list = new ArrayList<>(NetworkCableRenderer.connectors);
            for (final NetworkConnectorTileEntity connector : list) {
                final ChunkPos connectorChunkPos = new ChunkPos(connector.getBlockPos());
                if (Objects.equals(connectorChunkPos, chunkPos)) {
                    connectors.remove(connector);
                }
            }

            invalidateConnections();
        }
    }

    private static void handleWorldUnloadEvent(final WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            final LevelAccessor world = event.getWorld();

            final ArrayList<NetworkConnectorTileEntity> list = new ArrayList<>(NetworkCableRenderer.connectors);
            for (final NetworkConnectorTileEntity connector : list) {
                if (connector.getLevel() == world) {
                    connectors.remove(connector);
                }
            }

            invalidateConnections();
        }
    }

    private static void handleRenderWorld(final RenderLevelLastEvent event) {
        validateConnectors();
        validatePairs();

        if (Minecraft.useShaderTransparency()) {
            return;
        }

        if (connections.isEmpty()) {
            return;
        }

        final Minecraft client = Minecraft.getInstance();
        final Level world = client.level;
        if (world == null) {
            return;
        }

        final PoseStack matrixStack = event.getPoseStack();

        final Camera activeRenderInfo = client.gameRenderer.getMainCamera();
        final Vec3 eye = activeRenderInfo.getPosition();

        final Frustum frustum = new Frustum(matrixStack.last().pose(), event.getProjectionMatrix());
        frustum.prepare(eye.x, eye.y, eye.z);

        matrixStack.pushPose();
        matrixStack.translate(-eye.x, -eye.y, -eye.z);

        renderCables(world, matrixStack, eye, connections, frustum::isVisible);

        matrixStack.popPose();
    }

    private static void renderCables(final BlockAndTintGetter world, final PoseStack matrixStack, final Vec3 eye, final ArrayList<Connection> connections, final Predicate<AABB> filter) {
        final Matrix4f viewMatrix = matrixStack.last().pose();

        final RenderType renderType = ModRenderType.getNetworkCable();
        final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        final float r = CABLE_COLOR.x();
        final float g = CABLE_COLOR.y();
        final float b = CABLE_COLOR.z();

        for (final Connection connection : connections) {
            final Vec3 p0 = connection.from;
            final Vec3 p1 = connection.to;

            if (!p0.closerThan(eye, MAX_RENDER_DISTANCE) && !p1.closerThan(eye, MAX_RENDER_DISTANCE)) {
                continue;
            }

            // We may easily get false positives here for diagonal cables, but it's good enough for now.
            if (!filter.test(connection.bounds)) {
                continue;
            }

            final Vec3 p2 = animateCableSwing(
                lerp(p0, p1, 0.5f).subtract(0, computeCableHang(p0, p1), 0),
                connection.right,
                computeCableSwingAmount(p0, p1),
                connection.hashCode());

            final VertexConsumer buffer = bufferSource.getBuffer(renderType);

            cablePoints.clear();
            cablePoints.ensureCapacity(CABLE_VERTEX_COUNT);
            for (int i = 0; i < CABLE_VERTEX_COUNT; i++) {
                final float t = i / (CABLE_VERTEX_COUNT - 1f);
                final Vec3 p = quadraticBezier(p0, p1, p2, t);
                final Vec3 n = getExtrusionVector(eye, p, connection.forward);

                final BlockPos blockPos = new BlockPos(p);
                final int blockLight = world.getBrightness(LightLayer.BLOCK, blockPos);
                final int skyLight = world.getBrightness(LightLayer.SKY, blockPos);
                final int packedLight = LightTexture.pack(blockLight, skyLight);

                final Vector3f v0 = new Vector3f(p.subtract(n));
                final Vector3f v1 = new Vector3f(p.add(n));

                cablePoints.add(new CablePoint(v0, v1, packedLight));
            }

            for (int i = 0; i < cablePoints.size() - 1; i++) {
                final CablePoint pa = cablePoints.get(i);
                final CablePoint pb = cablePoints.get(i + 1);

                buffer.vertex(viewMatrix, pa.v0.x(), pa.v0.y(), pa.v0.z())
                    .color(r, g, b, 1f)
                    .uv2(pa.packedLight)
                    .endVertex();
                buffer.vertex(viewMatrix, pa.v1.x(), pa.v1.y(), pa.v1.z())
                    .color(r, g, b, 1f)
                    .uv2(pa.packedLight)
                    .endVertex();
                buffer.vertex(viewMatrix, pb.v1.x(), pb.v1.y(), pb.v1.z())
                    .color(r, g, b, 1f)
                    .uv2(pa.packedLight)
                    .endVertex();
                buffer.vertex(viewMatrix, pb.v0.x(), pb.v0.y(), pb.v0.z())
                    .color(r, g, b, 1f)
                    .uv2(pa.packedLight)
                    .endVertex();
            }

            bufferSource.endBatch(renderType);
        }
    }

    private static Vec3 lerp(final Vec3 a, final Vec3 b, final float t) {
        return a.add(b.subtract(a).scale(t)); // a + (b - a)*t = a*(1-t) + b*t
    }

    private static Vec3 quadraticBezier(final Vec3 a, final Vec3 b, final Vec3 c, final float t) {
        final Vec3 a1 = lerp(a, c, t);
        final Vec3 b1 = lerp(c, b, t);
        return lerp(a1, b1, t);
    }

    private static Vec3 getExtrusionVector(final Vec3 eye, final Vec3 v, final Vec3 forward) {
        return forward.cross(eye.subtract(v)).normalize().scale(CABLE_THICKNESS);
    }

    private static float computeCableHang(final Vec3 a, final Vec3 b) {
        final double length = a.distanceTo(b);
        final double hangFactor = Mth.clamp(length / CABLE_MAX_LENGTH, 0, 1);
        return (float) (CABLE_HANG_MIN + (CABLE_HANG_MAX - CABLE_HANG_MIN) * hangFactor);
    }

    private static float computeCableSwingAmount(final Vec3 p0, final Vec3 p1) {
        return Mth.clamp((float) p0.distanceTo(p1) / CABLE_LENGTH_FOR_MAX_SWING, 0.1f, 1f) * CABLE_MAX_SWING_AMOUNT;
    }

    private static Vec3 animateCableSwing(final Vec3 c, @Nullable final Vec3 right, final float swingAmount, final int seed) {
        final float relTime = ((System.currentTimeMillis() + seed) % CABLE_SWING_INTERVAL) / (float) CABLE_SWING_INTERVAL;
        final float relRadialTime = relTime * 2 * (float) Math.PI;

        if (right == null) {
            return c.add(swingAmount * Mth.sin(relRadialTime),
                0,
                swingAmount * Mth.cos(relRadialTime));
        } else {
            return c.add(swingAmount * Mth.cos(relRadialTime) * right.x,
                0.5f * swingAmount * Mth.sin(relRadialTime * 2 - (float) Math.PI) - swingAmount,
                swingAmount * Mth.cos(relRadialTime) * right.z);
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
            final BlockPos position = connector.getBlockPos();
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
        private static final Vec3 POS_Y = new Vec3(0, 1, 0);

        public final BlockPos fromPos, toPos;
        public final Vec3 from, to, forward, right;
        public final AABB bounds;

        private Connection(final BlockPos fromPos, final BlockPos toPos) {
            if (fromPos.compareTo(toPos) > 0) {
                this.fromPos = toPos;
                this.toPos = fromPos;
            } else {
                this.fromPos = fromPos;
                this.toPos = toPos;
            }

            from = Vec3.atCenterOf(fromPos);
            to = Vec3.atCenterOf(toPos);
            forward = to.subtract(from).normalize();
            right = fromPos.getX() == toPos.getX() && fromPos.getZ() == toPos.getZ()
                ? null : forward.cross(POS_Y);
            bounds = new AABB(from, to).inflate(0, CABLE_HANG_MAX, 0);
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

    private record CablePoint(Vector3f v0, Vector3f v1, int packedLight) { }
}
