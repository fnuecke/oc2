/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

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

    private static final Set<NetworkConnectorBlockEntity> connectors = Collections.newSetFromMap(new WeakHashMap<>());
    private static int lastKnownConnectorCount;
    private static boolean isDirty;

    private static final ArrayList<Connection> connections = new ArrayList<>();

    private static final float[] cableLeft = new float[CABLE_VERTEX_COUNT * 3];
    private static final float[] cableRight = new float[CABLE_VERTEX_COUNT * 3];
    private static final int[] cableLight = new int[CABLE_VERTEX_COUNT];
    private static final BlockPos.MutableBlockPos cablePos = new BlockPos.MutableBlockPos();

    // --------------------------------------------------------------------- //

    public static void addNetworkConnector(final NetworkConnectorBlockEntity connector) {
        connectors.add(connector);
        invalidateConnections();
    }

    public static void invalidateConnections() {
        isDirty = true;
    }

    // --------------------------------------------------------------------- //

    public static void onChunkUnload(final ChunkPos chunkPos) {
        removeConnectors(connector -> Objects.equals(new ChunkPos(connector.getBlockPos()), chunkPos));
    }

    public static void onLevelUnload(final LevelAccessor level) {
        removeConnectors(connector -> connector.getLevel() == level);
    }

    public static void render(final Camera camera, final Matrix4f modelViewMatrix, final Frustum frustum) {
        validateConnectors();
        validatePairs();

        if (connections.isEmpty()) {
            return;
        }

        final Minecraft client = Minecraft.getInstance();
        final Level level = client.level;
        if (level == null) {
            return;
        }

        final Vec3 eye = camera.getPosition();

        renderCables(level, modelViewMatrix, eye, connections, frustum::isVisible);
    }

    private static void renderCables(final BlockAndTintGetter level, final Matrix4f viewMatrix, final Vec3 eye, final ArrayList<Connection> connections, final Predicate<AABB> filter) {
        final RenderType renderType = ModRenderType.getNetworkCable();
        final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        final VertexConsumer consumer = bufferSource.getBuffer(renderType);

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

            buildCableOutline(level, eye, connection.forward, p0, p1, p2);

            for (int i = 0; i < CABLE_VERTEX_COUNT - 1; i++) {
                final int i0 = i * 3, i1 = (i + 1) * 3;
                final int light = cableLight[i];

                consumer.addVertex(viewMatrix, cableLeft[i0], cableLeft[i0 + 1], cableLeft[i0 + 2])
                    .setColor(r, g, b, 1f)
                    .setLight(light);
                consumer.addVertex(viewMatrix, cableRight[i0], cableRight[i0 + 1], cableRight[i0 + 2])
                    .setColor(r, g, b, 1f)
                    .setLight(light);
                consumer.addVertex(viewMatrix, cableRight[i1], cableRight[i1 + 1], cableRight[i1 + 2])
                    .setColor(r, g, b, 1f)
                    .setLight(light);
                consumer.addVertex(viewMatrix, cableLeft[i1], cableLeft[i1 + 1], cableLeft[i1 + 2])
                    .setColor(r, g, b, 1f)
                    .setLight(light);
            }
        }

        bufferSource.endBatch(renderType);
    }

    private static void buildCableOutline(final BlockAndTintGetter level, final Vec3 eye, final Vec3 forward,
                                          final Vec3 p0, final Vec3 p1, final Vec3 p2) {
        for (int i = 0; i < CABLE_VERTEX_COUNT; i++) {
            final float t = i / (CABLE_VERTEX_COUNT - 1f);

            final Vec3 p = quadraticBezier(p0, p1, p2, t);
            final Vec3 n = getExtrusionVector(eye, p, forward);

            cablePos.set(p.x, p.y, p.z);
            cableLight[i] = LightTexture.pack(
                level.getBrightness(LightLayer.BLOCK, cablePos),
                level.getBrightness(LightLayer.SKY, cablePos));

            final int o = i * 3;
            final double x = p.x - eye.x, y = p.y - eye.y, z = p.z - eye.z;
            cableLeft[o] = (float) (x - n.x);
            cableLeft[o + 1] = (float) (y - n.y);
            cableLeft[o + 2] = (float) (z - n.z);
            cableRight[o] = (float) (x + n.x);
            cableRight[o + 1] = (float) (y + n.y);
            cableRight[o + 2] = (float) (z + n.z);
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
        final float relTime = (System.currentTimeMillis() + seed) % CABLE_SWING_INTERVAL / (float) CABLE_SWING_INTERVAL;
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

    private static void removeConnectors(final Predicate<NetworkConnectorBlockEntity> filter) {
        boolean removedAny = false;
        for (final NetworkConnectorBlockEntity connector : new ArrayList<>(connectors)) {
            if (filter.test(connector)) {
                connectors.remove(connector);
                removedAny = true;
            }
        }

        if (removedAny) {
            invalidateConnections();
        }
    }

    private static void validateConnectors() {
        int count = 0;
        final var iterator = connectors.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isValid()) {
                count++;
            } else {
                iterator.remove();
                invalidateConnections();
            }
        }

        // We track the size because the WeakHasMap may expunge dead entries without
        // us knowing otherwise.
        if (count != lastKnownConnectorCount) {
            invalidateConnections();
        }
        lastKnownConnectorCount = count;
    }

    private static void validatePairs() {
        if (!isDirty) {
            return;
        }

        isDirty = false;
        connections.clear();

        final HashSet<Connection> seen = new HashSet<>();
        for (final NetworkConnectorBlockEntity connector : connectors) {
            final BlockPos position = connector.getBlockPos();
            for (final BlockPos connectedPosition : connector.getConnectedPositions()) {
                final Connection connection = new Connection(position, connectedPosition);
                if (seen.add(connection)) {
                    connections.add(connection);
                }
            }
        }
    }

    // --------------------------------------------------------------------- //

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
}
