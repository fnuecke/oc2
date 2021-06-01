package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.NetworkConnectorBlock;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.NetworkConnectorConnectionsMessage;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.ServerScheduler;
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
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


import javax.annotation.Nullable;
import java.util.*;

public final class NetworkConnectorBlockEntity extends AbstractBlockEntity implements TickableBlockEntity {
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

    private static final int RETRY_UNLOADED_CHUNK_INTERVAL = 5 * Constants.TICK_SECONDS;
    private static final int MAX_CONNECTION_COUNT = 2;
    private static final int MAX_CONNECTION_DISTANCE = 16;
    private static final int INITIAL_PACKET_TIME_TO_LIVE = 12;
    private static final int BYTES_PER_SECOND = 64 * 1024;
    private static final int BYTES_PER_TICK = BYTES_PER_SECOND / Constants.TICK_SECONDS;
    private static final int MIN_ETHERNET_FRAME_SIZE = 42;
    private static final int TTL_COST = 1;

    ///////////////////////////////////////////////////////////////////

    private final NetworkConnectorNetworkInterface networkInterface = new NetworkConnectorNetworkInterface();

    private Optional<NetworkInterface> localInterface = Optional.empty();
    private boolean isLocalConnectionDirty = true;

    private final HashSet<BlockPos> connectorPositions = new HashSet<>();
    private final HashSet<BlockPos> ownedCables = new HashSet<>();
    private final HashSet<BlockPos> dirtyConnectors = new HashSet<>();
    private final HashMap<BlockPos, NetworkConnectorBlockEntity> connectors = new HashMap<>();

    ///////////////////////////////////////////////////////////////////

    public NetworkConnectorBlockEntity() {
        super(TileEntities.NETWORK_CONNECTOR_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

    public static ConnectionResult connect(final NetworkConnectorBlockEntity connectorA, final NetworkConnectorBlockEntity connectorB) {
        if (connectorA == connectorB || connectorA.isRemoved() || connectorB.isRemoved()) {
            return ConnectionResult.FAILURE;
        }

        final Level world = connectorA.level;
        if (world == null || world.isClientSide) {
            return ConnectionResult.FAILURE;
        }

        if (connectorB.level != world) {
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

        if (isObstructed(world, posA, posB)) {
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

        if (!isRemoved()) {
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

    public void setLocalInterfaceChanged() {
        isLocalConnectionDirty = true;
    }


    public void setConnectedPositionsClient(final ArrayList<BlockPos> positions) {
        connectorPositions.clear();
        connectorPositions.addAll(positions);
        NetworkCableRenderer.invalidateConnections();
    }

    @Override
    public void tick() {
        if (isLocalConnectionDirty) {
            isLocalConnectionDirty = false;
            resolveLocalInterface();
        }

        if (!dirtyConnectors.isEmpty()) {
            final ArrayList<BlockPos> list = new ArrayList<>(dirtyConnectors);
            dirtyConnectors.clear();
            for (final BlockPos connectedPosition : list) {
                resolveConnectedInterface(connectedPosition);
            }
        }

        final NetworkInterface src = localInterface.orElse(NullNetworkInterface.INSTANCE);

        int byteBudget = BYTES_PER_TICK;
        byte[] frame;
        while ((frame = src.readEthernetFrame()) != null && byteBudget > 0) {
            byteBudget -= Math.max(frame.length, MIN_ETHERNET_FRAME_SIZE); // Avoid bogus packets messing with us.
            networkInterface.writeEthernetFrame(src, frame, INITIAL_PACKET_TIME_TO_LIVE);
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
    public void handleUpdateTag(final BlockState state, final CompoundTag tag) {
        super.handleUpdateTag(state, tag);

        final ListTag connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = connections.getCompound(i);
            final BlockPos position = NbtUtils.readBlockPos(connectionTag);
            connectorPositions.add(position);
            dirtyConnectors.add(position);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag = super.save(tag);

        final ListTag connections = new ListTag();
        for (final BlockPos position : connectorPositions) {
            final CompoundTag connectionTag = NbtUtils.writeBlockPos(position);
            if (ownedCables.contains(position)) {
                connectionTag.putBoolean(IS_OWNER_TAG_NAME, true);
            }
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG_NAME, connections);

        return tag;
    }

    @Override
    public void load(final BlockState state, final CompoundTag tag) {
        super.load(state, tag);

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
    protected void loadClient() {
        super.loadClient();

        NetworkCableRenderer.addNetworkConnector(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        // When we're being removed we want to break the actual link to any connected
        // connectors. This will also cause cables to be dropped.
        final ArrayList<NetworkConnectorBlockEntity> list = new ArrayList<>(connectors.values());
        connectors.clear();
        for (final NetworkConnectorBlockEntity connector : list) {
            disconnectFrom(connector.getBlockPos());
            connector.disconnectFrom(getBlockPos());
        }
    }

    @Override
    protected void unloadServer() {
        super.unloadServer();

        // When unloading, we just want to remove the reference to this tile entity
        // from connected connectors; we don't want to actually break the link.
        final BlockPos pos = getBlockPos();
        for (final NetworkConnectorBlockEntity connector : connectors.values()) {
            connector.connectors.remove(pos);
            if (connector.connectorPositions.contains(pos)) {
                connector.dirtyConnectors.add(pos);
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
            collector.offer(Capabilities.NETWORK_INTERFACE, networkInterface);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void resolveLocalInterface() {
        localInterface = Optional.empty();

        if (isRemoved()) {
            return;
        }

        if (level == null || level.isClientSide) {
            return;
        }

        final Direction facing = NetworkConnectorBlock.getFacing(getBlockState());
        final BlockPos sourcePos = getBlockPos().relative(facing.getOpposite());

        final ChunkPos sourceChunk = new ChunkPos(sourcePos);
        if (!level.hasChunk(sourceChunk.x, sourceChunk.z)) {
            ServerScheduler.schedule(level, this::setLocalInterfaceChanged, RETRY_UNLOADED_CHUNK_INTERVAL);
            return;
        }

        final BlockEntity tileEntity = level.getBlockEntity(sourcePos);
        if (tileEntity == null) {
            return;
        }

        localInterface = tileEntity.getCapability(Capabilities.NETWORK_INTERFACE, facing);
        if (localInterface.isPresent()) {
            localInterface.addListener(unused -> setLocalInterfaceChanged());
        }
    }

    private void resolveConnectedInterface(final BlockPos connectedPosition) {
        connectors.remove(connectedPosition);

        if (isRemoved()) {
            return;
        }

        if (level == null || level.isClientSide) {
            return;
        }

        final ChunkPos destinationChunk = new ChunkPos(connectedPosition);
        if (!level.hasChunk(destinationChunk.x, destinationChunk.z)) {
            ServerScheduler.schedule(level, () -> dirtyConnectors.add(connectedPosition), RETRY_UNLOADED_CHUNK_INTERVAL);
            return;
        }

        final BlockEntity tileEntity = level.getBlockEntity(connectedPosition);
        if (!(tileEntity instanceof NetworkConnectorBlockEntity)) {
            disconnectFrom(connectedPosition);
            return;
        }

        final NetworkConnectorBlockEntity connector = (NetworkConnectorBlockEntity) tileEntity;

        if (!connectedPosition.closerThan(getBlockPos(), MAX_CONNECTION_DISTANCE)) {
            disconnectFrom(connectedPosition);
            connector.disconnectFrom(getBlockPos());
            return;
        }

        if (isObstructed(level, getBlockPos(), connectedPosition)) {
            disconnectFrom(connectedPosition);
            connector.disconnectFrom(getBlockPos());
            return;
        }

        connectors.put(connectedPosition, connector);
    }

    private static boolean isObstructed(final Level world, final BlockPos a, final BlockPos b) {
        final Vec3 va = Vec3.atCenterOf(a);
        final Vec3 vb = Vec3.atCenterOf(b);
        final Vec3 ab = vb.subtract(va).normalize().scale(0.5);

        // Because of floating point inaccuracies the raytrace is not necessarily
        // symmetric. In particular when grazing corners perfectly, e.g. two connectors
        // attached to the same block at a 90 degree angle. So we check both ways.
        final BlockHitResult hitAB = world.clip(new ClipContext(
                va.add(ab),
                vb.subtract(ab),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        ));
        final BlockHitResult hitBA = world.clip(new ClipContext(
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

        if (level != null && !level.isClientSide) {
            final NetworkConnectorConnectionsMessage message = new NetworkConnectorConnectionsMessage(this);
            final LevelChunk chunk = level.getChunkAt(getBlockPos());
            Network.sendToClientsTrackingChunk(message, chunk);
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

            localInterface.ifPresent(dst -> {
                if (dst == source) {
                    return;
                }
                dst.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
            });

            for (final NetworkConnectorBlockEntity dst : connectors.values()) {
                if (dst.isRemoved() || dst.networkInterface == source) {
                    continue;
                }
                dst.networkInterface.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
            }
        }
    }
}
