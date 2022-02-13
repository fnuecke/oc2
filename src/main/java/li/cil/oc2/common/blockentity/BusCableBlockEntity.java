/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.bus.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusCableFacadeMessage;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class BusCableBlockEntity extends ModBlockEntity {
    public enum FacadeType {
        NOT_A_BLOCK,
        INVALID_BLOCK,
        VALID_BLOCK,
    }

    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String INTERFACE_NAMES_TAG_NAME = "interfaceNames";
    private static final String FACADE_TAG_NAME = "facade";

    ///////////////////////////////////////////////////////////////////

    private final AbstractBlockDeviceBusElement busElement = new BusCableBusElement();
    private final String[] interfaceNames = new String[Constants.BLOCK_FACE_COUNT];
    private final NeighborTracker[] neighborTrackers = new NeighborTracker[Constants.BLOCK_FACE_COUNT];
    private ItemStack facade = ItemStack.EMPTY;

    ///////////////////////////////////////////////////////////////////

    public BusCableBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.BUS_CABLE.get(), pos, state);

        for (final Direction side : Direction.values()) {
            neighborTrackers[side.get3DDataValue()] = new NeighborTracker(side);
        }
    }

    ///////////////////////////////////////////////////////////////////

    public String getInterfaceName(final Direction side) {
        final String interfaceName = interfaceNames[side.get3DDataValue()];
        return interfaceName == null ? "" : interfaceName;
    }

    public void setInterfaceName(final Direction side, final String name) {
        if (level == null) {
            return;
        }

        final String validatedName = validateName(name);
        if (Objects.equals(validatedName, interfaceNames[side.get3DDataValue()])) {
            return;
        }

        interfaceNames[side.get3DDataValue()] = validatedName;
        setChanged();

        if (!level.isClientSide()) {
            final BusInterfaceNameMessage message = new BusInterfaceNameMessage.ToClient(this, side, interfaceNames[side.get3DDataValue()]);
            Network.sendToClientsTrackingBlockEntity(message, this);
            busElement.updateDevicesForNeighbor(side);
        }
    }

    public FacadeType getFacadeType(final ItemStack stack) {
        return getFacadeType(ItemStackUtils.getBlockState(stack));
    }

    public FacadeType getFacadeType(@Nullable final BlockState state) {
        if (state == null) {
            return FacadeType.NOT_A_BLOCK;
        }

        if (level == null ||
            state.getRenderShape() != RenderShape.MODEL ||
            !state.isSolidRender(level, getBlockPos()) ||
            state.getBlock() instanceof EntityBlock) {
            return FacadeType.INVALID_BLOCK;
        }

        return FacadeType.VALID_BLOCK;
    }

    public ItemStack getFacade() {
        return facade;
    }

    public void setFacade(ItemStack stack) {
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(stack);
        if (getFacadeType(facadeState) != FacadeType.VALID_BLOCK) {
            stack = ItemStack.EMPTY;
        }

        if (ItemStack.isSame(stack, facade)) {
            return;
        }

        facade = stack.copy();
        facade.setCount(1);
        BusCableBlock.setHasFacade(level, getBlockPos(), getBlockState(), facadeState, true);

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

        if (!level.isClientSide()) {
            final BusCableFacadeMessage message = new BusCableFacadeMessage(getBlockPos(), facade);
            Network.sendToClientsTrackingBlockEntity(message, this);
        }
    }

    public void removeFacade() {
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(facade);
        facade = ItemStack.EMPTY;
        BusCableBlock.setHasFacade(level, getBlockPos(), getBlockState(), facadeState, false);

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

        if (!level.isClientSide()) {
            final BusCableFacadeMessage message = new BusCableFacadeMessage(getBlockPos(), facade);
            Network.sendToClientsTrackingBlockEntity(message, this);
        }
    }

    public void handleNeighborChanged(final BlockPos pos) {
        final BlockPos toPos = pos.subtract(getBlockPos());
        final Direction side = Direction.fromNormal(toPos.getX(), toPos.getY(), toPos.getZ());
        if (side != null) {
            busElement.updateDevicesForNeighbor(side);
        }
    }

    public void handleConfigurationChanged(@Nullable final Direction side, final boolean neighborConnectivityChanged) {
        if (side != null) {
            // Whenever the type changes we can clear it. Technically only needed
            // for the interface->none transition, but all others are no-ops, so
            // we can just do this.
            setInterfaceName(side, "");

            invalidateCapability(Capabilities.deviceBusElement(), side);

            final NeighborTracker tracker = neighborTrackers[side.get3DDataValue()];
            tracker.updateListener();
            tracker.scheduleNeighborDeviceUpdate();
        }

        if (neighborConnectivityChanged) {
            busElement.scheduleScan();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();

        tag.put(INTERFACE_NAMES_TAG_NAME, serializeInterfaceNames());
        tag.put(FACADE_TAG_NAME, facade.serializeNBT());

        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        deserializeInterfaceNames(tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        setFacade(ItemStack.of(tag.getCompound(FACADE_TAG_NAME)));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(BUS_ELEMENT_TAG_NAME, busElement.save());
        tag.put(INTERFACE_NAMES_TAG_NAME, serializeInterfaceNames());
        tag.put(FACADE_TAG_NAME, facade.serializeNBT());
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        busElement.load(tag.getCompound(BUS_ELEMENT_TAG_NAME));
        deserializeInterfaceNames(tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        facade = ItemStack.of(tag.getCompound(FACADE_TAG_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (BusCableBlock.getConnectionType(getBlockState(), direction) != BusCableBlock.ConnectionType.NONE) {
            collector.offer(Capabilities.deviceBusElement(), busElement);
        }
    }

    @Override
    protected void loadServer() {
        super.loadServer();

        for (final NeighborTracker tracker : neighborTrackers) {
            tracker.updateListener();
            tracker.scheduleNeighborDeviceUpdate();
        }

        scheduleBusScanInAdjacentBusElements();
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);

        if (isRemove) {
            busElement.setRemoved();
        }

        for (final NeighborTracker tracker : neighborTrackers) {
            tracker.close();
        }
    }

    ///////////////////////////////////////////////////////////////////

    private ListTag serializeInterfaceNames() {
        final ListTag tag = new ListTag();
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            tag.add(StringTag.valueOf(getInterfaceName(Direction.from3DDataValue(i))));
        }
        return tag;
    }

    private void deserializeInterfaceNames(final ListTag tag) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            final String name = tag.getString(i).trim();
            interfaceNames[i] = name.substring(0, Math.min(32, name.length()));
        }
    }

    private static String validateName(final String name) {
        final String trimmed = name.trim();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }

    private void scheduleBusScanInAdjacentBusElements() {
        // This is called from onLoad, so we cannot access neighbors yet.
        assert level != null;
        ServerScheduler.schedule(level, () -> {
            if (!isValid()) {
                return;
            }

            final Level level = requireNonNull(getLevel());
            final BlockPos pos = getBlockPos();
            for (final Direction direction : Constants.DIRECTIONS) {
                final BlockPos neighborPos = pos.relative(direction);
                final BlockEntity blockEntity = LevelUtils.getBlockEntityIfChunkExists(level, neighborPos);
                if (blockEntity == null) {
                    continue;
                }

                final LazyOptional<DeviceBusElement> capability = blockEntity
                    .getCapability(Capabilities.deviceBusElement(), direction.getOpposite());
                capability.ifPresent(DeviceBus::scheduleScan);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////

    private final class BusCableBusElement extends AbstractBlockDeviceBusElement {
        @Nullable
        @Override
        public LevelAccessor getLevel() {
            return BusCableBlockEntity.this.getLevel();
        }

        @Override
        public BlockPos getPosition() {
            return getBlockPos();
        }

        @Override
        public boolean canScanContinueTowards(@Nullable final Direction direction) {
            final BusCableBlock.ConnectionType connectionType = BusCableBlock.getConnectionType(getBlockState(), direction);
            return connectionType == BusCableBlock.ConnectionType.CABLE ||
                connectionType == BusCableBlock.ConnectionType.INTERFACE;
        }

        @Override
        public boolean canDetectDevicesTowards(@Nullable final Direction direction) {
            final BusCableBlock.ConnectionType connectionType = BusCableBlock.getConnectionType(getBlockState(), direction);
            return connectionType == BusCableBlock.ConnectionType.INTERFACE;
        }

        @Override
        protected void collectSyntheticDevices(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side, final HashSet<BlockEntry> entries) {
            super.collectSyntheticDevices(level, pos, side, entries);

            if (side == null || entries.isEmpty()) {
                return;
            }

            final String interfaceName = interfaceNames[side.get3DDataValue()];
            if (!StringUtil.isNullOrEmpty(interfaceName)) {
                entries.add(new BlockEntry(new BlockDeviceInfo(null, new TypeNameRPCDevice(interfaceName)), side));
            }
        }

        @Override
        public double getEnergyConsumption() {
            return super.getEnergyConsumption()
                + Config.busCableEnergyPerTick
                + BusCableBlock.getInterfaceCount(getBlockState()) * Config.busInterfaceEnergyPerTick;
        }
    }

    /**
     * Utility class to track neighboring blocks, per side.
     * <p>
     * Since we manage devices for blocks that may not even be aware of this, we need to actively
     * track their presence and state. There are to major cases:
     * <ul>
     * <li>The neighboring block is in the same chunk as the cable. In this case, we only need to
     * listen to neighbor block changes (via {@link #handleNeighborChanged(BlockPos)}).</li>
     * <li>The neighboring block is in another chunk as the cable. In this case, we also need to
     * track chunk load status, since we won't get any other event in case the block gets
     * loaded or unloaded.</li>
     * </ul>
     * The second case is handled by this class.
     * <p>
     * To avoid unnecessary overhead, we only track neighbors which actually are in another chunk,
     * and to which this cable has a Bus Interface. As such, configuration changes need to update
     * listeners. This is done in {@link #handleConfigurationChanged(Direction, boolean)} by calling
     * {@link #updateListener()}.
     * <p>
     * We initialize our state from {@link #loadServer()}, where all trackers are updated.
     */
    private final class NeighborTracker implements AutoCloseable {
        private final Runnable onChunkLoadedStateChanged = this::handleChunkLoadOrUnload;

        final Direction side;
        final EnumProperty<BusCableBlock.ConnectionType> connectionProperty;
        private final ChunkPos chunkPos;
        private final boolean isSameChunk;
        private boolean hasRegisteredListener;
        private boolean hasScheduledUpdate;

        public NeighborTracker(final Direction side) {
            this.side = side;
            connectionProperty = BusCableBlock.FACING_TO_CONNECTION_MAP.get(side);
            chunkPos = new ChunkPos(getBlockPos().relative(side));
            isSameChunk = Objects.equals(new ChunkPos(getBlockPos()), chunkPos);
        }

        public void close() {
            removeListener();
        }

        public void scheduleNeighborDeviceUpdate() {
            if (level != null && !hasScheduledUpdate) {
                ServerScheduler.schedule(level, this::updateNeighborDevices);
                hasScheduledUpdate = true;
            }
        }

        public void updateListener() {
            if (isSameChunk) {
                return;
            }

            final boolean needsListener = getBlockState().getValue(connectionProperty) == BusCableBlock.ConnectionType.INTERFACE;
            if (needsListener && !hasRegisteredListener) {
                addListener();
            } else if (!needsListener && hasRegisteredListener) {
                removeListener();
            }
        }

        private void addListener() {
            if (level != null && !hasRegisteredListener) {
                ServerScheduler.subscribeOnLoad(level, chunkPos, onChunkLoadedStateChanged);
                ServerScheduler.subscribeOnUnload(level, chunkPos, onChunkLoadedStateChanged);
            }
            hasRegisteredListener = true;
        }

        private void removeListener() {
            if (level != null && hasRegisteredListener) {
                ServerScheduler.unsubscribeOnLoad(level, chunkPos, onChunkLoadedStateChanged);
                ServerScheduler.unsubscribeOnUnload(level, chunkPos, onChunkLoadedStateChanged);
            }
            hasRegisteredListener = false;
        }

        private void handleChunkLoadOrUnload() {
            // Don't directly run a device update, as this may cause deadlocks.
            scheduleNeighborDeviceUpdate();
        }

        private void updateNeighborDevices() {
            if (isValid()) {
                busElement.updateDevicesForNeighbor(side);
            }
            hasScheduledUpdate = false;
        }
    }
}
