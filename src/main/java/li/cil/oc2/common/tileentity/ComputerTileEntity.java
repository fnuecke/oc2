package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.DeviceItemStackHandler;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerBootErrorMessage;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.ComputerTerminalOutputMessage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.vm.*;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class ComputerTileEntity extends AbstractTileEntity implements ITickableTileEntity {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";

    private static final int MEMORY_SLOTS = 4;
    private static final int HARD_DRIVE_SLOTS = 4;
    private static final int FLASH_MEMORY_SLOTS = 1;
    private static final int CARD_SLOTS = 4;

    ///////////////////////////////////////////////////////////////////

    private boolean hasAddedOwnDevices;
    private boolean isNeighborUpdateScheduled;

    ///////////////////////////////////////////////////////////////////

    private final Terminal terminal = new Terminal();
    private final TileEntityDeviceBusElement busElement = new ComputerBusElement();

    private final ComputerVirtualMachineState state;
    private final ComputerItemStackHandlers items = new ComputerItemStackHandlers();

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntity() {
        super(TileEntities.COMPUTER_TILE_ENTITY.get());

        // We want to unload devices even on world unload to free global resources.
        setNeedsWorldUnloadEvent();

        final ComputerBusController busController = new ComputerBusController(busElement);
        state = new ComputerVirtualMachineState(busController, new CommonVirtualMachine(busController));
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public VirtualMachineState getState() {
        return state;
    }

    public VirtualMachineItemStackHandlers getItemStackHandlers() {
        return items;
    }

    public void start() {
        final World world = getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        state.start();
    }

    public void stop() {
        final World world = getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        state.stop();
    }

    public void handleNeighborChanged() {
        state.busController.scheduleBusScan();
    }

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
        for (final Device device : state.busController.getDevices()) {
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
        final World world = getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        // Always add devices provided for the computer itself, even if there's no
        // adjacent cable. Because that would just be weird.
        if (!hasAddedOwnDevices) {
            hasAddedOwnDevices = true;
            for (final LazyOptional<BlockDeviceInfo> optional : Devices.getDevices(this, (Direction) null)) {
                optional.ifPresent(info -> busElement.addDevice(info.device));
            }
        }

        if (isNeighborUpdateScheduled) {
            isNeighborUpdateScheduled = false;
            world.notifyNeighborsOfStateChange(getPos(), getBlockState().getBlock());
        }

        state.tick();
    }

    @Override
    public void remove() {
        super.remove();

        // Unload only suspends, but we want to do a full clean-up when we get
        // destroyed, so stuff inside us can delete out-of-nbt persisted runtime-
        // only data such as ram.
        state.virtualMachine.vmAdapter.unload();
    }

    @Override
    public CompoundNBT getUpdateTag() {
        final CompoundNBT tag = super.getUpdateTag();

        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        tag.putInt(AbstractVirtualMachineState.BUS_STATE_TAG_NAME, state.getBusState().ordinal());
        tag.putInt(AbstractVirtualMachineState.RUN_STATE_TAG_NAME, state.getRunState().ordinal());
        tag.putString(AbstractVirtualMachineState.BOOT_ERROR_TAG_NAME, ITextComponent.Serializer.toJson(state.getBootError()));

        return tag;
    }

    @Override
    public void handleUpdateTag(final BlockState blockState, final CompoundNBT tag) {
        super.handleUpdateTag(blockState, tag);

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        state.setBusStateClient(AbstractDeviceBusController.BusState.values()[tag.getInt(AbstractVirtualMachineState.BUS_STATE_TAG_NAME)]);
        state.setRunStateClient(VirtualMachineState.RunState.values()[tag.getInt(AbstractVirtualMachineState.RUN_STATE_TAG_NAME)]);
        state.setBootErrorClient(ITextComponent.Serializer.getComponentFromJson(tag.getString(AbstractVirtualMachineState.BOOT_ERROR_TAG_NAME)));
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag = super.write(tag);

        tag.put(STATE_TAG_NAME, state.serialize());
        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        tag.put(BUS_ELEMENT_TAG_NAME, NBTSerialization.serialize(busElement));
        tag.put(Constants.INVENTORY_TAG_NAME, items.serialize());

        return tag;
    }

    @Override
    public void read(final BlockState blockState, final CompoundNBT tag) {
        super.read(blockState, tag);

        state.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);

        if (tag.contains(BUS_ELEMENT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(tag.getCompound(BUS_ELEMENT_TAG_NAME), busElement);
        }

        if (tag.contains(Constants.INVENTORY_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            items.deserialize(tag.getCompound(Constants.INVENTORY_TAG_NAME));
        }
    }

    public void exportToItemStack(final ItemStack stack) {
        items.exportToItemStack(stack);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.ITEM_HANDLER, items.itemHandlers);
        collector.offer(Capabilities.DEVICE_BUS_ELEMENT, busElement);
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
        state.virtualMachine.rtcMinecraft.setWorld(getWorld());
    }

    @Override
    protected void unloadServer() {
        super.unloadServer();

        state.joinVirtualMachine();
        state.virtualMachine.vmAdapter.suspend();

        state.busController.dispose();

        // This is necessary in case some other controller found us before our controller
        // did its scan, which can happen because the scan can happen with a delay.
        busElement.scheduleScan();
    }

    ///////////////////////////////////////////////////////////////////

    private final class ComputerItemStackHandlers extends AbstractVirtualMachineItemStackHandlers {
        public ComputerItemStackHandlers() {
            super(MEMORY_SLOTS, HARD_DRIVE_SLOTS, FLASH_MEMORY_SLOTS, CARD_SLOTS);
        }

        @Override
        protected List<ItemDeviceInfo> getDevices(final ItemStack stack) {
            return Devices.getDevices(ComputerTileEntity.this, stack);
        }

        @Override
        protected void onContentsChanged(final DeviceItemStackHandler itemStackHandler, final int slot) {
            super.onContentsChanged(itemStackHandler, slot);
            markDirty();
            isNeighborUpdateScheduled = true;
        }
    }

    private final class ComputerBusController extends TileEntityDeviceBusController {
        private ComputerBusController(final DeviceBusElement root) {
            super(root, ComputerTileEntity.this);
        }

        @Override
        protected void onBeforeScan() {
            state.reload();
            state.virtualMachine.rpcAdapter.pause();
        }

        @Override
        protected void onAfterDeviceScan(final boolean didDevicesChange) {
            state.virtualMachine.rpcAdapter.resume(didDevicesChange);
        }

        @Override
        protected void onDevicesAdded(final Collection<Device> devices) {
            state.virtualMachine.vmAdapter.addDevices(devices);
        }

        @Override
        protected void onDevicesRemoved(final Collection<Device> devices) {
            state.virtualMachine.vmAdapter.removeDevices(devices);
        }
    }

    private final class ComputerBusElement extends TileEntityDeviceBusElement {
        public ComputerBusElement() {
            super(ComputerTileEntity.this);
        }

        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return super.getNeighbors().map(neighbors -> {
                final ArrayList<LazyOptional<DeviceBusElement>> list = new ArrayList<>(neighbors);
                list.add(LazyOptional.of(() -> items.busElement));
                return list;
            });
        }

        @Override
        public boolean canScanContinueTowards(@Nullable final Direction direction) {
            return getBlockState().get(ComputerBlock.HORIZONTAL_FACING) != direction;
        }
    }

    private final class ComputerVirtualMachineRunner extends AbstractTerminalVirtualMachineRunner {
        public ComputerVirtualMachineRunner(final CommonVirtualMachine virtualMachine, final Terminal terminal) {
            super(virtualMachine, terminal);
        }

        @Override
        protected void sendTerminalUpdateToClient(final ByteBuffer output) {
            final ComputerTerminalOutputMessage message = new ComputerTerminalOutputMessage(ComputerTileEntity.this, output);
            Network.sendToClientsTrackingChunk(message, state.chunk);
        }
    }

    private final class ComputerVirtualMachineState extends AbstractVirtualMachineState<ComputerBusController, CommonVirtualMachine> {
        private Chunk chunk;

        private ComputerVirtualMachineState(final ComputerBusController busController, final CommonVirtualMachine virtualMachine) {
            super(busController, virtualMachine);
            virtualMachine.vmAdapter.setDefaultAddressProvider(items::getDefaultDeviceAddress);
        }

        @Override
        public void tick() {
            if (chunk == null) {
                chunk = world.getChunkAt(getPos());
            }

            if (isRunning()) {
                chunk.markDirty();
            }

            super.tick();
        }

        @Override
        protected AbstractTerminalVirtualMachineRunner createRunner() {
            return new ComputerVirtualMachineRunner(virtualMachine, terminal);
        }

        @Override
        protected void handleBusStateChanged(final AbstractDeviceBusController.BusState value) {
            Network.sendToClientsTrackingChunk(new ComputerBusStateMessage(ComputerTileEntity.this), chunk);

            if (value == AbstractDeviceBusController.BusState.READY) {
                // Bus just became ready, meaning new devices may be available, meaning new
                // capabilities may be available, so we need to tell our neighbors.
                world.notifyNeighborsOfStateChange(getPos(), getBlockState().getBlock());
            }
        }

        @Override
        protected void handleRunStateChanged(final RunState value) {
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
