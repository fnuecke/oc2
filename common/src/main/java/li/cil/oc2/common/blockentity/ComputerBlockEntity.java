/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import dev.architectury.utils.EnvExecutor;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.client.audio.LoopingSoundManager;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.bus.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.bus.BlockDeviceBusController;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityProvider;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.container.ComputerInventoryContainer;
import li.cil.oc2.common.container.ComputerTerminalContainer;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.*;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.*;
import li.cil.oc2.common.vm.*;
import net.fabricmc.api.EnvType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;

public final class ComputerBlockEntity extends ModBlockEntity implements TerminalUserProvider, TickableBlockEntity {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String ENERGY_TAG_NAME = "energy";

    private static final int MEMORY_SLOTS = 4;
    private static final int HARD_DRIVE_SLOTS = 4;
    private static final int FLASH_MEMORY_SLOTS = 1;
    private static final int CARD_SLOTS = 4;

    private static final int MAX_RUNNING_SOUND_DELAY = TickUtils.toTicks(Duration.ofSeconds(2));

    private static final int TERMINAL_RECIPIENT_REFRESH_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(1));
    private static final double TERMINAL_VIEW_DISTANCE = 8;

    // ------------------------------------------------------------- //

    private boolean hasAddedOwnDevices;
    private boolean isNeighborUpdateScheduled;
    private LevelChunk chunk;

    // ------------------------------------------------------------- //

    private final Terminal terminal = new Terminal();
    private final ComputerBusElement busElement = new ComputerBusElement();
    private final ComputerItemStackHandlers deviceItems = new ComputerItemStackHandlers();
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.computerEnergyStorage);
    private final ComputerVirtualMachine virtualMachine = new ComputerVirtualMachine(new BlockDeviceBusController(busElement, Config.computerEnergyPerTick, this), deviceItems::getDeviceAddressBase);
    private final Set<Player> terminalUsers = Collections.newSetFromMap(new WeakHashMap<>());
    private volatile List<ServerPlayer> terminalRecipients = List.of(); // Players to send live terminal updates to.
    private int terminalRecipientRefreshCountdown;

    // ------------------------------------------------------------- //

    public ComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.COMPUTER.get(), pos, state);

        // We want to unload devices even on level unload to free global resources.
        setNeedsLevelUnloadEvent();

        // Device scan can change our capability set (cards in the computer having capabilities).
        virtualMachine.busController.onAfterDeviceScan.add(this::handleAfterDeviceScan);
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public VMItemStackHandlers getItemStackHandlers() {
        return deviceItems;
    }

    public void start() {
        if (level != null && !level.isClientSide()) {
            virtualMachine.start();
        }
    }

    public void stop() {
        if (level != null && !level.isClientSide()) {
            virtualMachine.stop();
        }
    }

    public void openTerminalScreen(final ServerPlayer player) {
        ComputerTerminalContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void openInventoryScreen(final ServerPlayer player) {
        ComputerInventoryContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void addTerminalUser(final Player player) {
        terminalUsers.add(player);
        updateTerminalRecipients();
    }

    public void removeTerminalUser(final Player player) {
        terminalUsers.remove(player);
        updateTerminalRecipients();
    }

    @Override
    public Iterable<Player> getTerminalUsers() {
        return terminalUsers;
    }

    public void handleNeighborChanged() {
        if (level != null && !level.isClientSide()) {
            virtualMachine.busController.scheduleBusScan();
        }
    }

    @Nullable
    @Override
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (!isValid()) {
            return null;
        }

        final T own = super.getCapability(capability, side);
        if (own != null) {
            return own;
        }

        final Direction localSide = HorizontalBlockUtils.toLocal(getBlockState(), side);
        for (final Device device : virtualMachine.busController.getDevices()) {
            if (device instanceof final CapabilityProvider capabilityProvider) {
                final T value = capabilityProvider.getCapability(capability, localSide);
                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

    @Override
    public void clientTick() {
        terminal.clientTick();
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }

        // Always add devices provided for the computer itself, even if there's no
        // adjacent cable. Because that would just be weird.
        if (!hasAddedOwnDevices) {
            hasAddedOwnDevices = true;
            busElement.addOwnDevices();
        }

        if (isNeighborUpdateScheduled) {
            isNeighborUpdateScheduled = false;
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }

        if (--terminalRecipientRefreshCountdown <= 0) {
            terminalRecipientRefreshCountdown = TERMINAL_RECIPIENT_REFRESH_INTERVAL;
            updateTerminalRecipients();
        }

        // Just grab it again every tick, to avoid this becoming invalid if something tries to
        // mess with this BlockEntity in unexpected ways.
        chunk = level.getChunkAt(getBlockPos());

        virtualMachine.tick();
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);

        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        tag.putInt(AbstractVirtualMachine.BUS_STATE_TAG_NAME, virtualMachine.getBusState().ordinal());
        tag.putInt(AbstractVirtualMachine.RUN_STATE_TAG_NAME, virtualMachine.getRunState().ordinal());
        final Component bootError = virtualMachine.getBootError();
        if (bootError != null) {
            tag.putString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME, Component.Serializer.toJson(bootError, registries));
        }

        return tag;
    }


    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (virtualMachine.getRunState() != VMRunState.STOPPED) {
            tag.put(STATE_TAG_NAME, virtualMachine.serialize());
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }

        tag.put(ENERGY_TAG_NAME, energy.serializeNBT());
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.save());
        tag.put(ITEMS_TAG_NAME, deviceItems.saveItems(registries));
        tag.put(DEVICES_TAG_NAME, deviceItems.saveDevices());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        energy.deserializeNBT(tag.getCompound(ENERGY_TAG_NAME));
        virtualMachine.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);

        // Client-visible state, only present in the tag produced by getUpdateTag().
        if (tag.contains(AbstractVirtualMachine.BUS_STATE_TAG_NAME)) {
            EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> {
                virtualMachine.setBusStateClient(CommonDeviceBusController.BusState.values()[tag.getInt(AbstractVirtualMachine.BUS_STATE_TAG_NAME)]);
                virtualMachine.setRunStateClient(VMRunState.values()[tag.getInt(AbstractVirtualMachine.RUN_STATE_TAG_NAME)]);
                virtualMachine.setBootErrorClient(tag.contains(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME)
                        ? Component.Serializer.fromJson(tag.getString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME), registries)
                        : null);
            });
        }
        busElement.load(tag.getCompound(BUS_ELEMENT_TAG_NAME));

        deviceItems.loadItems(registries, tag.getCompound(ITEMS_TAG_NAME));
        deviceItems.loadDevices(tag.getCompound(DEVICES_TAG_NAME));
    }

    public void exportToItemStack(final ItemStack stack) {
        final HolderLookup.Provider registries = requireNonNull(getLevel()).registryAccess();
        ItemStackUtils.modifyBlockEntityDataTag(stack, getType(), tag -> {
            deviceItems.saveItems(registries, NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME));
            tag.put(ENERGY_TAG_NAME, energy.serializeNBT());
        });
    }

    // ------------------------------------------------------------- //

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.ITEM_HANDLER, deviceItems.combinedItemHandlers);
        collector.offer(Capabilities.DEVICE_BUS_ELEMENT, busElement);
        collector.offer(Capabilities.TERMINAL_USER_PROVIDER, this);

        if (Config.computersUseEnergy()) {
            collector.offer(Capabilities.ENERGY_STORAGE, energy);
        }
    }

    @Override
    protected void loadClient() {
        super.loadClient();

        terminal.setDisplayOnly(true);
    }

    @Override
    protected void loadServer() {
        super.loadServer();

        assert level != null;

        virtualMachine.state.builtinDevices.rtcMinecraft.setLevel(level);
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);

        if (isRemove) {
            virtualMachine.stop();
        } else {
            virtualMachine.suspend();
        }

        virtualMachine.dispose();

        // This is necessary in case some other controller found us before our controller
        // did its scan, which can happen because the scan can happen with a delay. In
        // that case we don't know that controller and disposing our controller won't
        // notify it, so we also send out a notification through our bus element, which
        // would be registered with other controllers in that case.
        busElement.scheduleScan();
    }

    // ------------------------------------------------------------- //

    private void updateTerminalRecipients() {
        if (!(level instanceof final ServerLevel serverLevel)) {
            return;
        }

        // Players in range who might be looking at the computer.
        final AABB bounds = AABB.ofSize(Vec3.atCenterOf(getBlockPos()),
                TERMINAL_VIEW_DISTANCE * 2, TERMINAL_VIEW_DISTANCE * 2, TERMINAL_VIEW_DISTANCE * 2);
        final Set<ServerPlayer> recipients = new LinkedHashSet<>(serverLevel.getEntitiesOfClass(ServerPlayer.class, bounds));

        // Players who have the UI open. Don't ask me how they'd be out of range, but hey, paranoia.
        for (final Player player : terminalUsers) {
            if (player instanceof final ServerPlayer serverPlayer) {
                recipients.add(serverPlayer);
            }
        }

        final List<ServerPlayer> previous = terminalRecipients;
        terminalRecipients = List.copyOf(recipients);

        // Send full update to new ones.
        for (final ServerPlayer player : terminalRecipients) {
            if (!previous.contains(player)) {
                player.connection.send(ClientboundBlockEntityDataPacket.create(this));
            }
        }
    }

    private void sendToTerminalRecipients(final AbstractMessage message) {
        for (final ServerPlayer player : terminalRecipients) {
            Network.sendToClient(message, player);
        }
    }

    private <T extends AbstractMessage> void sendToClientsTrackingComputer(final T message) {
        if (chunk != null) {
            Network.sendToClientsTrackingChunk(message, chunk);
        }
    }

    private void handleAfterDeviceScan(final CommonDeviceBusController.AfterDeviceScanEvent event) {
        if (event.didDevicesChange() && level != null && !level.isClientSide()) {
            Capabilities.invalidate(this);
        }
    }

    // ------------------------------------------------------------- //

    private final class ComputerItemStackHandlers extends AbstractVMItemStackHandlers {
        public ComputerItemStackHandlers() {
            super(new GroupDefinition(DeviceTypes.MEMORY, MEMORY_SLOTS), new GroupDefinition(DeviceTypes.HARD_DRIVE, HARD_DRIVE_SLOTS), new GroupDefinition(DeviceTypes.FLASH_MEMORY, FLASH_MEMORY_SLOTS), new GroupDefinition(DeviceTypes.CARD, CARD_SLOTS));
        }

        @Override
        protected ItemDeviceQuery makeQuery(final ItemStack stack) {
            return Devices.makeQuery(ComputerBlockEntity.this, stack);
        }

        @Override
        protected void onChanged() {
            super.onChanged();
            if (level != null && !level.isClientSide()) {
                virtualMachine.busController.scheduleBusScan();
                ChunkUtils.setLazyUnsaved(level, getBlockPos());
            }
            isNeighborUpdateScheduled = true;
        }
    }

    private final class ComputerBusElement extends AbstractBlockDeviceBusElement {
        private static final String DEVICE_ID_TAG_NAME = "device_id";

        private final HashSet<Device> devices = new HashSet<>();
        private UUID deviceId = UUID.randomUUID();

        @Nullable
        @Override
        public LevelAccessor getLevel() {
            return ComputerBlockEntity.this.getLevel();
        }

        @Override
        public BlockPos getPosition() {
            return getBlockPos();
        }

        public void addOwnDevices() {
            assert level != null;

            collectDevices(level, getPosition(), null).ifPresent(result -> {
                for (final BlockEntry info : result.getEntries()) {
                    devices.add(info.getDevice());
                    super.addDevice(info.getDevice());
                }
            });
        }

        @Override
        public Optional<Collection<Invalidatable<DeviceBusElement>>> getNeighbors() {
            return super.getNeighbors().map(neighbors -> {
                // If we have valid neighbors (complete bus) also add a connection to the bus
                // element hosting our item devices.
                final ArrayList<Invalidatable<DeviceBusElement>> list = new ArrayList<>(neighbors);
                list.add(Invalidatable.of(deviceItems.busElement));
                return list;
            });
        }

        @Override
        public boolean canScanContinueTowards(@Nullable final Direction direction) {
            return getBlockState().getValue(ComputerBlock.FACING) != direction;
        }

        @Override
        protected boolean canDetectDevicesTowards(@Nullable final Direction direction) {
            return direction == null;
        }

        @Override
        public Optional<UUID> getDeviceIdentifier(final Device device) {
            if (devices.contains(device)) {
                return Optional.of(deviceId);
            }
            return super.getDeviceIdentifier(device);
        }

        @Override
        public CompoundTag save() {
            final CompoundTag tag = super.save();
            tag.putUUID(DEVICE_ID_TAG_NAME, deviceId);
            return tag;
        }

        public void load(final CompoundTag tag) {
            super.load(tag);
            if (tag.hasUUID(DEVICE_ID_TAG_NAME)) {
                deviceId = tag.getUUID(DEVICE_ID_TAG_NAME);
            }
        }
    }

    private final class ComputerVMRunner extends AbstractTerminalVMRunner {
        public ComputerVMRunner(final AbstractVirtualMachine virtualMachine, final Terminal terminal) {
            super(virtualMachine, terminal);
        }

        @Override
        protected void sendTerminalUpdateToClient(final ByteBuffer output) {
            sendToTerminalRecipients(new ComputerTerminalOutputMessage(ComputerBlockEntity.this, output));
        }
    }

    private final class ComputerVirtualMachine extends AbstractVirtualMachine {
        private ComputerVirtualMachine(final CommonDeviceBusController busController, final BaseAddressProvider baseAddressProvider) {
            super(busController);
            state.vmAdapter.setBaseAddressProvider(baseAddressProvider);
        }

        @Override
        public void setRunStateClient(final VMRunState value) {
            super.setRunStateClient(value);

            if (value == VMRunState.RUNNING) {
                if (!LoopingSoundManager.isPlaying(ComputerBlockEntity.this) && level != null) {
                    LoopingSoundManager.play(ComputerBlockEntity.this, SoundEvents.COMPUTER_RUNNING.get(), level.getRandom().nextInt(MAX_RUNNING_SOUND_DELAY));
                }
            } else {
                LoopingSoundManager.stop(ComputerBlockEntity.this);
            }
        }

        @Override
        public void tick() {
            assert level != null;

            if (isRunning()) {
                ChunkUtils.setLazyUnsaved(level, getBlockPos());
                busController.setDeviceContainersChanged();
            }

            super.tick();
        }

        @Override
        protected boolean consumeEnergy(final int amount, final boolean simulate) {
            if (!Config.computersUseEnergy()) {
                return true;
            }

            if (amount > energy.getEnergyStored()) {
                return false;
            }

            energy.extractEnergy(amount, simulate);
            return true;
        }

        @Override
        protected void stopRunnerAndReset() {
            super.stopRunnerAndReset();

            TerminalUtils.resetTerminal(terminal, output -> sendToTerminalRecipients(new ComputerTerminalOutputMessage(ComputerBlockEntity.this, output)));
        }

        @Override
        protected AbstractTerminalVMRunner createRunner() {
            return new ComputerVMRunner(this, terminal);
        }

        @Override
        protected void handleBusStateChanged(final CommonDeviceBusController.BusState value) {
            sendToClientsTrackingComputer(new ComputerBusStateMessage(ComputerBlockEntity.this, value));

            if (value == CommonDeviceBusController.BusState.READY && level != null) {
                // Bus just became ready, meaning new devices may be available, meaning new
                // capabilities may be available, so we need to tell our neighbors.
                level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            }
        }

        @Override
        protected void handleRunStateChanged(final VMRunState value) {
            sendToClientsTrackingComputer(new ComputerRunStateMessage(ComputerBlockEntity.this, value));
        }

        @Override
        protected void handleBootErrorChanged(@Nullable final Component value) {
            sendToClientsTrackingComputer(new ComputerBootErrorMessage(ComputerBlockEntity.this, value));
        }
    }
}
