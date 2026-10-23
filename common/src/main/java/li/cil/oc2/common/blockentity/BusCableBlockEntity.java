/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import com.mojang.serialization.Codec;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.bus.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.bus.device.TypeNameDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusCableFacadeMessage;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.sedna.api.device.bus.DeviceDescription;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final Codec<List<String>> INTERFACE_NAMES_CODEC = Codec.STRING.listOf();

    // --------------------------------------------------------------------- //

    private final AbstractBlockDeviceBusElement busElement = new BusCableBusElement();
    private final String[] interfaceNames = new String[Constants.BLOCK_FACE_COUNT];
    private final NeighborTracker[] neighborTrackers = new NeighborTracker[Constants.BLOCK_FACE_COUNT];
    private ItemStack facade = ItemStack.EMPTY;

    // --------------------------------------------------------------------- //

    public BusCableBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.BUS_CABLE.get(), pos, state);

        for (final Direction side : Direction.values()) {
            neighborTrackers[side.get3DDataValue()] = new NeighborTracker(side);
        }
    }

    // --------------------------------------------------------------------- //

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
            !state.isSolidRender() ||
            state.getBlock() instanceof EntityBlock) {
            return FacadeType.INVALID_BLOCK;
        }

        return FacadeType.VALID_BLOCK;
    }

    public ItemStack getFacade() {
        return facade;
    }

    public void setFacade(final ItemStack stack) {
        if (level == null) {
            return;
        }

        if (stack.isEmpty()) {
            removeFacade();
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(stack);
        if (getFacadeType(facadeState) != FacadeType.VALID_BLOCK
            || ItemStack.isSameItemSameComponents(stack, facade)) {
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
        if (level == null || facade.isEmpty()) {
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

    public void handleNeighborChanged(final Direction side) {
        busElement.updateDevicesForNeighbor(side);
    }

    public void handleConfigurationChanged(@Nullable final Direction side, final boolean neighborConnectivityChanged) {
        if (side != null) {
            // Whenever the type changes we can clear it. Technically only needed
            // for the interface->none transition, but all others are no-ops, so
            // we can just do this.
            setInterfaceName(side, "");

            Capabilities.invalidate(this);

            final NeighborTracker tracker = neighborTrackers[side.get3DDataValue()];
            tracker.updateListener();
            tracker.scheduleNeighborDeviceUpdate();
        }

        if (neighborConnectivityChanged) {
            busElement.scheduleScan();
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);

        tag.store(INTERFACE_NAMES_TAG_NAME, INTERFACE_NAMES_CODEC, serializeInterfaceNames());
        tag.store(FACADE_TAG_NAME, ItemStack.OPTIONAL_CODEC, registries.createSerializationContext(NbtOps.INSTANCE), facade);

        return tag;
    }


    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        busElement.save(output.child(BUS_ELEMENT_TAG_NAME));
        output.store(INTERFACE_NAMES_TAG_NAME, INTERFACE_NAMES_CODEC, serializeInterfaceNames());
        output.store(FACADE_TAG_NAME, ItemStack.OPTIONAL_CODEC, facade);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        busElement.load(input.childOrEmpty(BUS_ELEMENT_TAG_NAME));
        deserializeInterfaceNames(input.read(INTERFACE_NAMES_TAG_NAME, INTERFACE_NAMES_CODEC).orElse(List.of()));
        facade = input.read(FACADE_TAG_NAME, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (BusCableBlock.getConnectionType(getBlockState(), direction) != BusCableBlock.ConnectionType.NONE) {
            collector.offer(Capabilities.DEVICE_BUS_ELEMENT, busElement);
        }
    }

    @Override
    protected void loadServer() {
        super.loadServer();

        for (final NeighborTracker tracker : neighborTrackers) {
            tracker.updateListener();
            tracker.scheduleNeighborDeviceUpdate();
        }
    }

    @Override
    protected void loadServerInLoadedLevel() {
        super.loadServerInLoadedLevel();

        scanAdjacentBusElements();
    }

    @Override
    protected void unloadServer(final boolean isDestroyed) {
        super.unloadServer(isDestroyed);

        if (isDestroyed) {
            busElement.setRemoved();
        } else {
            busElement.dropNeighborHandles();
        }

        for (final NeighborTracker tracker : neighborTrackers) {
            tracker.close();
        }
    }

    // --------------------------------------------------------------------- //

    private List<String> serializeInterfaceNames() {
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            names.add(getInterfaceName(Direction.from3DDataValue(i)));
        }
        return names;
    }

    private void deserializeInterfaceNames(final List<String> names) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            interfaceNames[i] = validateName(i < names.size() ? names.get(i) : "");
        }
    }

    private static String validateName(final String name) {
        final String trimmed = name.trim();
        final String capped = trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
        return DeviceDescription.isValidName(capped) ? capped : "";
    }

    private void scanAdjacentBusElements() {
        final Level level = requireNonNull(getLevel());
        final BlockPos pos = getBlockPos();
        for (final Direction direction : Constants.DIRECTIONS) {
            final BlockPos neighborPos = pos.relative(direction);
            final BlockEntity blockEntity = LevelUtils.getBlockEntityIfChunkExists(level, neighborPos);
            if (blockEntity == null) {
                continue;
            }

            final DeviceBusElement busElement = Capabilities.get(blockEntity, Capabilities.DEVICE_BUS_ELEMENT, direction.getOpposite());
            if (busElement != null) {
                busElement.scheduleScan();
            }
        }
    }

    // --------------------------------------------------------------------- //

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
        protected void collectSyntheticDevices(final ServerLevel level, final BlockPos pos, @Nullable final Direction side, final HashSet<BlockEntry> entries) {
            super.collectSyntheticDevices(level, pos, side, entries);

            if (side == null || entries.isEmpty()) {
                return;
            }

            final String interfaceName = interfaceNames[side.get3DDataValue()];
            if (!StringUtil.isNullOrEmpty(interfaceName)) {
                entries.add(new BlockEntry(new BlockDeviceInfo(null, new TypeNameDevice(interfaceName)), side));
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
     * listen to neighbor block changes (via {@link #handleNeighborChanged(Direction)}).</li>
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
