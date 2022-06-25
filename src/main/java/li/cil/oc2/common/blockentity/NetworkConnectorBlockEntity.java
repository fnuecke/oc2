/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.NetworkConnectorBlock;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.NetworkConnectorConnectionsMessage;
import li.cil.oc2.common.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public final class NetworkConnectorBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    public enum ConnectionResult {
        SUCCESS,
        FAILURE,
        FAILURE_FULL,
        FAILURE_TOO_FAR,
        FAILURE_OBSTRUCTED,
        ALREADY_CONNECTED
    }

    private static final String CONNECTIONS_TAG_NAME = "connections";
    private static final String IS_OWNER_TAG_NAME = "is_owner";

    private static final int RETRY_UNLOADED_CHUNK_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(5));
    private static final int MAX_CONNECTION_COUNT = 2;
    private static final int MAX_CONNECTION_DISTANCE = 16;
    private static final int BYTES_PER_TICK = 64 * 1024 / TickUtils.toTicks(Duration.ofSeconds(1)); // bytes / sec -> bytes / tick
    private static final int MIN_ETHERNET_FRAME_SIZE = 42;
    private static final int TTL_COST = 1;

    ///////////////////////////////////////////////////////////////////

    private final NetworkConnectorNetworkInterface networkInterface = new NetworkConnectorNetworkInterface();

    private LazyOptional<NetworkInterface> adjacentInterface = LazyOptional.empty();
    private boolean isAdjacentInterfaceDirty = true;

    private final HashSet<BlockPos> connectorPositions = new HashSet<>();
    private final HashSet<BlockPos> ownedCables = new HashSet<>();
    private final HashSet<BlockPos> dirtyConnectors = new HashSet<>();
    private final HashMap<BlockPos, NetworkConnectorBlockEntity> connectors = new HashMap<>();

    ///////////////////////////////////////////////////////////////////

    public NetworkConnectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.NETWORK_CONNECTOR.get(), pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    public static ConnectionResult connect(final NetworkConnectorBlockEntity connectorA, final NetworkConnectorBlockEntity connectorB) {
        if (connectorA == connectorB || !connectorA.isValid() || !connectorB.isValid()) {
            return ConnectionResult.FAILURE;
        }

        final Level level = connectorA.level;
        if (level == null || level.isClientSide()) {
            return ConnectionResult.FAILURE;
        }

        if (connectorB.level != level) {
            return ConnectionResult.FAILURE;
        }

        if (!connectorA.canConnectMore() || !connectorB.canConnectMore()) {
            return ConnectionResult.FAILURE_FULL;
        }

        final BlockPos posA = connectorA.getBlockPos();
        final BlockPos posB = connectorB.getBlockPos();

        if (!posA.closerThan(posB, MAX_CONNECTION_DISTANCE)) {
            return ConnectionResult.FAILURE_TOO_FAR;
        }

        if (isObstructed(level, posA, posB)) {
            return ConnectionResult.FAILURE_OBSTRUCTED;
        }

        if (connectorA.connectorPositions.add(posB)) {
            connectorA.dirtyConnectors.add(posB);
            connectorA.onConnectedPositionsChanged();
        }

        if (connectorB.connectorPositions.add(posA)) {
            connectorB.dirtyConnectors.add(posA);
            connectorB.onConnectedPositionsChanged();
        }

        final ConnectionResult result;
        if (connectorA.ownedCables.contains(posB) || connectorB.ownedCables.contains(posA)) {
            connectorA.ownedCables.add(posB);
            connectorB.ownedCables.remove(posA);
            result = ConnectionResult.ALREADY_CONNECTED;
        } else {
            connectorA.ownedCables.add(posB);
            result = ConnectionResult.SUCCESS;
        }

        connectorA.setChanged();
        connectorB.setChanged();

        return result;
    }

    public void disconnectFrom(final BlockPos pos) {
        dirtyConnectors.remove(pos);
        connectors.remove(pos);

        if (ownedCables.remove(pos)) {
            if (level != null) {
                final Vec3 middle = Vec3.atCenterOf(getBlockPos().offset(pos)).scale(0.5f);
                ItemStackUtils.spawnAsEntity(level, middle, new ItemStack(Items.NETWORK_CABLE.get()));
            }
        }

        if (isValid()) {
            if (connectorPositions.remove(pos)) {
                onConnectedPositionsChanged();
            }

            setChanged();
        }
    }

    public boolean canConnectMore() {
        return connectorPositions.size() < MAX_CONNECTION_COUNT;
    }

    public Collection<BlockPos> getConnectedPositions() {
        return connectorPositions;
    }

    public void setNeighborChanged() {
        isAdjacentInterfaceDirty = true;
    }

    @OnlyIn(Dist.CLIENT)
    public void setConnectedPositionsClient(final ArrayList<BlockPos> positions) {
        connectorPositions.clear();
        connectorPositions.addAll(positions);
        NetworkCableRenderer.invalidateConnections();
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }

        if (isAdjacentInterfaceDirty) {
            isAdjacentInterfaceDirty = false;
            resolveLocalInterface();
        }

        if (!dirtyConnectors.isEmpty()) {
            final ArrayList<BlockPos> list = new ArrayList<>(dirtyConnectors);
            dirtyConnectors.clear();
            for (final BlockPos connectedPosition : list) {
                resolveConnectedInterface(connectedPosition);
            }
        }

        final NetworkInterface source = adjacentInterface.orElse(NullNetworkInterface.INSTANCE);

        int byteBudget = BYTES_PER_TICK;
        byte[] frame;
        while ((frame = source.readEthernetFrame()) != null && byteBudget > 0) {
            byteBudget -= Math.max(frame.length, MIN_ETHERNET_FRAME_SIZE); // Avoid bogus packets messing with us.
            networkInterface.writeEthernetFrame(source, frame, Config.ethernetFrameTimeToLive);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();

        final ListTag connections = new ListTag();
        for (final BlockPos position : connectorPositions) {
            final CompoundTag connectionTag = NbtUtils.writeBlockPos(position);
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG_NAME, connections);

        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        super.handleUpdateTag(tag);

        final ListTag connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = connections.getCompound(i);
            final BlockPos position = NbtUtils.readBlockPos(connectionTag);
            connectorPositions.add(position);
            dirtyConnectors.add(position);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);

        final ListTag connections = new ListTag();
        for (final BlockPos position : connectorPositions) {
            final CompoundTag connectionTag = NbtUtils.writeBlockPos(position);
            if (ownedCables.contains(position)) {
                connectionTag.putBoolean(IS_OWNER_TAG_NAME, true);
            }
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG_NAME, connections);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);

        final ListTag connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = connections.getCompound(i);
            final BlockPos position = NbtUtils.readBlockPos(connectionTag);
            connectorPositions.add(position);
            dirtyConnectors.add(position);
            if (connectionTag.getBoolean(IS_OWNER_TAG_NAME)) {
                ownedCables.add(position);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (Minecraft.useShaderTransparency()) {
            return new AABB(
                getBlockPos().offset(-MAX_CONNECTION_DISTANCE, -MAX_CONNECTION_DISTANCE, -MAX_CONNECTION_DISTANCE),
                getBlockPos().offset(1 + MAX_CONNECTION_DISTANCE, 1 + MAX_CONNECTION_DISTANCE, 1 + MAX_CONNECTION_DISTANCE)
            );
        } else {
            return super.getRenderBoundingBox();
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (direction == NetworkConnectorBlock.getFacing(getBlockState()).getOpposite()) {
            collector.offer(Capabilities.networkInterface(), networkInterface);
        }
    }

    @Override
    protected void loadClient() {
        super.loadClient();

        NetworkCableRenderer.addNetworkConnector(this);
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);

        if (isRemove) {
            // When we're being removed we want to break the actual link to any connected
            // connectors. This will also cause cables to be dropped.
            final ArrayList<NetworkConnectorBlockEntity> list = new ArrayList<>(connectors.values());
            connectors.clear();
            for (final NetworkConnectorBlockEntity connector : list) {
                disconnectFrom(connector.getBlockPos());
                connector.disconnectFrom(getBlockPos());
            }
        } else {
            // When unloading, we just want to remove the reference to this block entity
            // from connected connectors; we don't want to actually break the link.
            final BlockPos pos = getBlockPos();
            for (final NetworkConnectorBlockEntity connector : connectors.values()) {
                connector.connectors.remove(pos);
                if (connector.connectorPositions.contains(pos)) {
                    connector.dirtyConnectors.add(pos);
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void resolveLocalInterface() {
        assert level != null;

        adjacentInterface = LazyOptional.empty();

        if (!isValid()) {
            return;
        }

        final Direction facing = NetworkConnectorBlock.getFacing(getBlockState());
        final BlockPos sourcePos = getBlockPos().relative(facing.getOpposite());

        if (!level.isLoaded(sourcePos)) {
            ServerScheduler.schedule(level, this::setNeighborChanged, RETRY_UNLOADED_CHUNK_INTERVAL);
            return;
        }

        final BlockEntity blockEntity = level.getBlockEntity(sourcePos);
        if (blockEntity == null) {
            return;
        }

        adjacentInterface = blockEntity.getCapability(Capabilities.networkInterface(), facing);
        if (adjacentInterface.isPresent()) {
            LazyOptionalUtils.addWeakListener(adjacentInterface, this, (connector, unused) -> connector.setNeighborChanged());
        }
    }

    private void resolveConnectedInterface(final BlockPos connectedPosition) {
        connectors.remove(connectedPosition);

        if (!isValid()) {
            return;
        }

        if (level == null || level.isClientSide()) {
            return;
        }

        final ChunkPos destinationChunk = new ChunkPos(connectedPosition);
        if (!level.hasChunk(destinationChunk.x, destinationChunk.z)) {
            ServerScheduler.schedule(level, () -> dirtyConnectors.add(connectedPosition), RETRY_UNLOADED_CHUNK_INTERVAL);
            return;
        }

        final BlockEntity blockEntity = level.getBlockEntity(connectedPosition);
        if (!(blockEntity instanceof final NetworkConnectorBlockEntity networkConnector)) {
            disconnectFrom(connectedPosition);
            return;
        }

        if (!connectedPosition.closerThan(getBlockPos(), MAX_CONNECTION_DISTANCE)) {
            disconnectFrom(connectedPosition);
            networkConnector.disconnectFrom(getBlockPos());
            return;
        }

        if (isObstructed(level, getBlockPos(), connectedPosition)) {
            disconnectFrom(connectedPosition);
            networkConnector.disconnectFrom(getBlockPos());
            return;
        }

        connectors.put(connectedPosition, networkConnector);
    }

    private static boolean isObstructed(final Level level, final BlockPos a, final BlockPos b) {
        final Vec3 va = Vec3.atCenterOf(a);
        final Vec3 vb = Vec3.atCenterOf(b);
        final Vec3 ab = vb.subtract(va).normalize().scale(0.5);

        // Because of floating point inaccuracies the raytrace is not necessarily
        // symmetric. In particular when grazing corners perfectly, e.g. two connectors
        // attached to the same block at a 90-degree angle. So we check both ways.
        final BlockHitResult hitAB = level.clip(new ClipContext(
            va.add(ab),
            vb.subtract(ab),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            null
        ));
        final BlockHitResult hitBA = level.clip(new ClipContext(
            vb.subtract(ab),
            va.add(ab),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            null
        ));

        return hitAB.getType() != HitResult.Type.MISS ||
            hitBA.getType() != HitResult.Type.MISS;
    }

    private void onConnectedPositionsChanged() {
        if (level != null && !level.isClientSide()) {
            final NetworkConnectorConnectionsMessage message = new NetworkConnectorConnectionsMessage(this);
            Network.sendToClientsTrackingBlockEntity(message, this);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class NullNetworkInterface implements NetworkInterface {
        public static final NetworkInterface INSTANCE = new NullNetworkInterface();

        @Override
        public byte[] readEthernetFrame() {
            return null;
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        }
    }

    private final class NetworkConnectorNetworkInterface implements NetworkInterface {
        @Override
        public byte[] readEthernetFrame() {
            return null;
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
            if (timeToLive <= 0) {
                return;
            }

            adjacentInterface.ifPresent(dst -> {
                if (dst == source) {
                    return;
                }
                dst.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
            });

            for (final NetworkConnectorBlockEntity dst : connectors.values()) {
                if (!dst.isValid() || dst.networkInterface == source) {
                    continue;
                }
                dst.networkInterface.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
            }
        }
    }
}
