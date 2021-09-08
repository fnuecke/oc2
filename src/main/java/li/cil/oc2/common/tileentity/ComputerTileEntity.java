package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.client.audio.LoopingSoundManager;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.ComputerInventoryContainer;
import li.cil.oc2.common.container.ComputerTerminalContainer;
import li.cil.oc2.common.container.DeviceItemStackHandler;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerBootErrorMessage;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.ComputerTerminalOutputMessage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.TerminalUtils;
import li.cil.oc2.common.vm.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;

import static li.cil.oc2.common.Constants.*;

public final class ComputerTileEntity extends AbstractTileEntity implements ITickableTileEntity, TerminalUserProvider {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String ENERGY_TAG_NAME = "energy";

    private static final int MEMORY_SLOTS = 4;
    private static final int HARD_DRIVE_SLOTS = 4;
    private static final int FLASH_MEMORY_SLOTS = 1;
    private static final int CARD_SLOTS = 4;

    private static final int MAX_RUNNING_SOUND_DELAY = SECONDS_TO_TICKS * 2;

    ///////////////////////////////////////////////////////////////////

    private boolean hasAddedOwnDevices;
    private boolean isNeighborUpdateScheduled;

    ///////////////////////////////////////////////////////////////////

    private final Terminal terminal = new Terminal();
    private final ComputerBusElement busElement = new ComputerBusElement();
    private final ComputerItemStackHandlers deviceItems = new ComputerItemStackHandlers();
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.computerEnergyStorage);
    private final ComputerVirtualMachine virtualMachine = new ComputerVirtualMachine(new TileEntityDeviceBusController(busElement, Config.computerEnergyPerTick, this), deviceItems::getDeviceAddressBase);
    private final Set<PlayerEntity> terminalUsers = Collections.newSetFromMap(new WeakHashMap<>());

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntity() {
        super(TileEntities.COMPUTER_TILE_ENTITY.get());

        // We want to unload devices even on world unload to free global resources.
        setNeedsWorldUnloadEvent();
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
        if (level == null || level.isClientSide()) {
            return;
        }

        virtualMachine.start();
    }

    public void stop() {
        if (level == null || level.isClientSide()) {
            return;
        }

        virtualMachine.stop();
    }

    public void openTerminalScreen(final ServerPlayerEntity player) {
        ComputerTerminalContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void openInventoryScreen(final ServerPlayerEntity player) {
        ComputerInventoryContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void addTerminalUser(final PlayerEntity player) {
        terminalUsers.add(player);
    }

    public void removeTerminalUser(final PlayerEntity player) {
        terminalUsers.remove(player);
    }

    @Override
    public Iterable<PlayerEntity> getTerminalUsers() {
        return terminalUsers;
    }

    public void handleNeighborChanged() {
        virtualMachine.busController.scheduleBusScan();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction side) {
        if (isRemoved()) {
            return LazyOptional.empty();
        }

        final LazyOptional<T> optional = super.getCapability(capability, side);
        if (optional.isPresent()) {
            return optional;
        }

        final Direction localSide = HorizontalBlockUtils.toLocal(getBlockState(), side);
        for (final Device device : virtualMachine.busController.getDevices()) {
            if (device instanceof ICapabilityProvider) {
                final LazyOptional<T> value = ((ICapabilityProvider) device).getCapability(capability, localSide);
                if (value.isPresent()) {
                    return value;
                }
            }
        }

        return LazyOptional.empty();
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide()) {
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

        virtualMachine.tick();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        // super.remove() calls onUnload. This in turn only suspends, but we want to do
        // a full clean-up when we get destroyed, so stuff inside us can delete out-of-nbt
        // persisted runtime-only data such as ram.
        virtualMachine.state.vmAdapter.unmount();
    }

    @Override
    public CompoundNBT getUpdateTag() {
        final CompoundNBT tag = super.getUpdateTag();

        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        tag.putInt(AbstractVirtualMachine.BUS_STATE_TAG_NAME, virtualMachine.getBusState().ordinal());
        tag.putInt(AbstractVirtualMachine.RUN_STATE_TAG_NAME, virtualMachine.getRunState().ordinal());
        tag.putString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME, ITextComponent.Serializer.toJson(virtualMachine.getBootError()));

        return tag;
    }

    @Override
    public void handleUpdateTag(final BlockState blockState, final CompoundNBT tag) {
        super.handleUpdateTag(blockState, tag);

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        virtualMachine.setBusStateClient(CommonDeviceBusController.BusState.values()[tag.getInt(AbstractVirtualMachine.BUS_STATE_TAG_NAME)]);
        virtualMachine.setRunStateClient(VMRunState.values()[tag.getInt(AbstractVirtualMachine.RUN_STATE_TAG_NAME)]);
        virtualMachine.setBootErrorClient(ITextComponent.Serializer.fromJson(tag.getString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME)));
    }

    @Override
    public CompoundNBT save(final CompoundNBT tag) {
        super.save(tag);

        tag.put(ENERGY_TAG_NAME, energy.serializeNBT());
        tag.put(STATE_TAG_NAME, virtualMachine.serialize());
        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.save());
        tag.put(ITEMS_TAG_NAME, deviceItems.saveItems());
        tag.put(DEVICES_TAG_NAME, deviceItems.saveDevices());

        return tag;
    }

    @Override
    public void load(final BlockState blockState, final CompoundNBT tag) {
        super.load(blockState, tag);

        energy.deserializeNBT(tag.getCompound(ENERGY_TAG_NAME));
        virtualMachine.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        busElement.load(tag.getCompound(BUS_ELEMENT_TAG_NAME));

        deviceItems.loadItems(tag.getCompound(ITEMS_TAG_NAME));
        deviceItems.loadDevices(tag.getCompound(DEVICES_TAG_NAME));
    }

    public void exportToItemStack(final ItemStack stack) {
        deviceItems.saveItems(NBTUtils.getOrCreateChildTag(stack.getOrCreateTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME));
    }

    ///////////////////////////////////////////////////////////////////

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

        busElement.initialize();
        virtualMachine.state.builtinDevices.rtcMinecraft.setWorld(level);
    }

    @Override
    protected void unloadServer() {
        super.unloadServer();

        virtualMachine.suspend();

        // This is necessary in case some other controller found us before our controller
        // did its scan, which can happen because the scan can happen with a delay. In
        // that case we don't know that controller and disposing our controller won't
        // notify it, so we also send out a notification through our bus element, which
        // would be registered with other controllers in that case.
        busElement.scheduleScan();
    }

    ///////////////////////////////////////////////////////////////////

    private final class ComputerItemStackHandlers extends AbstractVMItemStackHandlers {
        public ComputerItemStackHandlers() {
            super(
                    GroupDefinition.of(DeviceTypes.MEMORY, MEMORY_SLOTS),
                    GroupDefinition.of(DeviceTypes.HARD_DRIVE, HARD_DRIVE_SLOTS),
                    GroupDefinition.of(DeviceTypes.FLASH_MEMORY, FLASH_MEMORY_SLOTS),
                    GroupDefinition.of(DeviceTypes.CARD, CARD_SLOTS)
            );
        }

        @Override
        protected ItemDeviceQuery getDeviceQuery(final ItemStack stack) {
            return Devices.makeQuery(ComputerTileEntity.this, stack);
        }

        @Override
        protected void onContentsChanged(final DeviceItemStackHandler itemStackHandler, final int slot) {
            super.onContentsChanged(itemStackHandler, slot);
            setChanged();
            isNeighborUpdateScheduled = true;
        }
    }

    private final class ComputerBusElement extends TileEntityDeviceBusElement {
        private static final String DEVICE_ID_TAG_NAME = "device_id";

        private final HashSet<Device> devices = new HashSet<>();
        private UUID deviceId = UUID.randomUUID();

        public ComputerBusElement() {
            super(ComputerTileEntity.this);
        }

        public void addOwnDevices() {
            for (final BlockDeviceInfo info : collectDevices(level, getPosition(), null)) {
                devices.add(info.device);
                super.addDevice(info.device);
            }
        }

        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return super.getNeighbors().map(neighbors -> {
                // If we have valid neighbors (complete bus) also add a connection to the bus
                // element hosting our item devices.
                final ArrayList<LazyOptional<DeviceBusElement>> list = new ArrayList<>(neighbors);
                list.add(LazyOptional.of(() -> deviceItems.busElement));
                return list;
            });
        }

        @Override
        public boolean canScanContinueTowards(@Nullable final Direction direction) {
            return getBlockState().getValue(ComputerBlock.FACING) != direction;
        }

        @Override
        public Optional<UUID> getDeviceIdentifier(final Device device) {
            if (devices.contains(device)) {
                return Optional.of(deviceId);
            }
            return super.getDeviceIdentifier(device);
        }

        @Override
        public CompoundNBT save() {
            final CompoundNBT tag = super.save();
            tag.putUUID(DEVICE_ID_TAG_NAME, deviceId);
            return tag;
        }

        public void load(final CompoundNBT tag) {
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
            Network.sendToClientsTrackingChunk(new ComputerTerminalOutputMessage(ComputerTileEntity.this, output), virtualMachine.chunk);
        }
    }

    private final class ComputerVirtualMachine extends AbstractVirtualMachine {
        private Chunk chunk;

        private ComputerVirtualMachine(final CommonDeviceBusController busController, final BaseAddressProvider baseAddressProvider) {
            super(busController);
            state.vmAdapter.setBaseAddressProvider(baseAddressProvider);
        }

        @Override
        public void setRunStateClient(final VMRunState value) {
            super.setRunStateClient(value);

            if (value == VMRunState.RUNNING) {
                if (!LoopingSoundManager.isPlaying(ComputerTileEntity.this)) {
                    LoopingSoundManager.play(ComputerTileEntity.this, SoundEvents.COMPUTER_RUNNING.get(), level.getRandom().nextInt(MAX_RUNNING_SOUND_DELAY));
                }
            } else {
                LoopingSoundManager.stop(ComputerTileEntity.this);
            }
        }

        @Override
        public void tick() {
            if (chunk == null) {
                chunk = level.getChunkAt(getBlockPos());
            }

            if (isRunning()) {
                chunk.markUnsaved();
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

            TerminalUtils.resetTerminal(terminal, output -> Network.sendToClientsTrackingChunk(
                    new ComputerTerminalOutputMessage(ComputerTileEntity.this, output), chunk));
        }

        @Override
        protected AbstractTerminalVMRunner createRunner() {
            return new ComputerVMRunner(this, terminal);
        }

        @Override
        protected void handleBusStateChanged(final CommonDeviceBusController.BusState value) {
            Network.sendToClientsTrackingChunk(new ComputerBusStateMessage(ComputerTileEntity.this), chunk);

            if (value == CommonDeviceBusController.BusState.READY) {
                // Bus just became ready, meaning new devices may be available, meaning new
                // capabilities may be available, so we need to tell our neighbors.
                level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            }
        }

        @Override
        protected void handleRunStateChanged(final VMRunState value) {
            // This method can be called from disposal logic, so if we are disposed quickly enough
            // chunk may not be initialized yet. Avoid resulting NRE in network logic.
            if (chunk != null) {
                Network.sendToClientsTrackingChunk(new ComputerRunStateMessage(ComputerTileEntity.this), chunk);
            }
        }

        @Override
        protected void handleBootErrorChanged(@Nullable final ITextComponent value) {
            Network.sendToClientsTrackingChunk(new ComputerBootErrorMessage(ComputerTileEntity.this), chunk);
        }
    }
}
