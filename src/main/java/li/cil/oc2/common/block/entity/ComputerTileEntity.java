package li.cil.oc2.common.block.entity;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.Constants;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLifecycleEventType;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.bus.device.ItemDeviceInfo;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.DeviceItemStackHandler;
import li.cil.oc2.common.init.TileEntities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.TerminalBlockOutputMessage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.VirtualMachineRunner;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;
import li.cil.sedna.fs.HostFileSystem;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

public final class ComputerTileEntity extends AbstractTileEntity implements ITickableTileEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final String BUS_ELEMENT_NBT_TAG_NAME = "busElement";
    private static final String BUS_STATE_NBT_TAG_NAME = "busState";
    private static final String TERMINAL_NBT_TAG_NAME = "terminal";
    private static final String VIRTUAL_MACHINE_NBT_TAG_NAME = "virtualMachine";
    private static final String RUNNER_NBT_TAG_NAME = "runner";
    private static final String RUN_STATE_NBT_TAG_NAME = "runState";

    private static final int DEVICE_LOAD_RETRY_INTERVAL = 10 * 20; // In ticks.

    private static final long ITEM_DEVICE_BASE_ADDRESS = 0x40000000L;
    private static final int ITEM_DEVICE_STRIDE = 0x1000;

    ///////////////////////////////////////////////////////////////////

    public enum RunState {
        STOPPED,
        LOADING_DEVICES,
        RUNNING,
    }

    ///////////////////////////////////////////////////////////////////

    private Chunk chunk;
    private final AbstractDeviceBusController busController;
    private AbstractDeviceBusController.BusState busState;
    private RunState runState;
    private int loadDevicesDelay;

    ///////////////////////////////////////////////////////////////////

    private final DeviceItemStackHandler itemHandler = new DeviceItemStackHandler(8);
    private final Terminal terminal = new Terminal();
    private final TileEntityDeviceBusElement busElement = new BusElement();
    private final ComputerVirtualMachine virtualMachine;
    private ComputerVirtualMachineRunner runner;

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntity() {
        super(TileEntities.COMPUTER_TILE_ENTITY.get());

        // We want to unload devices even on world unload to free global resources.
        setNeedsWorldUnloadEvent();

        busController = new BusController(busElement);
        busState = AbstractDeviceBusController.BusState.SCAN_PENDING;
        runState = RunState.STOPPED;

        virtualMachine = new ComputerVirtualMachine(busController);
        virtualMachine.vmAdapter.setDefaultAddressProvider(this::getDefaultDeviceAddress);
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public void start() {
        if (runState == RunState.RUNNING) {
            return;
        }

        setRunState(RunState.LOADING_DEVICES);
        loadDevicesDelay = 0;
    }

    public void stop() {
        if (runState == RunState.STOPPED) {
            return;
        }

        if (runState == RunState.LOADING_DEVICES) {
            setRunState(RunState.STOPPED);
            return;
        }

        stopRunnerAndResetVM();
    }

    public boolean isRunning() {
        return getBusState() == AbstractDeviceBusController.BusState.READY &&
               getRunState() == RunState.RUNNING;
    }

    public AbstractDeviceBusController.BusState getBusState() {
        return busState;
    }

    public RunState getRunState() {
        return runState;
    }

    public void handleNeighborChanged(final BlockPos pos) {
        busElement.handleNeighborChanged(pos);
    }

    @OnlyIn(Dist.CLIENT)
    public void setRunStateClient(final RunState value) {
        final World world = getWorld();
        if (world != null && world.isRemote()) {
            runState = value;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setBusStateClient(final AbstractDeviceBusController.BusState value) {
        final World world = getWorld();
        if (world != null && world.isRemote()) {
            busState = value;
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(final @NotNull Capability<T> capability, @Nullable final Direction side) {
        final LazyOptional<T> optional = super.getCapability(capability, side);
        if (optional.isPresent()) {
            return optional;
        }

        final Direction localSide = HorizontalBlockUtils.toLocal(getBlockState(), side);
        for (final Device device : busController.getDevices()) {
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
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (direction != getBlockState().get(ComputerBlock.HORIZONTAL_FACING)) {
            collector.offer(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY, busElement);
            collector.offer(Capabilities.DEVICE_BUS_CONTROLLER_CAPABILITY, busController);
        }

        collector.offer(Capabilities.ITEM_HANDLER_CAPABILITY, itemHandler);
    }

    @Override
    public void tick() {
        final World world = getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        if (chunk == null) {
            chunk = world.getChunkAt(getPos());
        }

        busController.scan();
        setBusState(busController.getState());
        if (busState != AbstractDeviceBusController.BusState.READY) {
            return;
        }

        switch (runState) {
            case STOPPED:
                break;
            case LOADING_DEVICES:
                if (loadDevicesDelay > 0) {
                    loadDevicesDelay--;
                    break;
                }

                if (!virtualMachine.vmAdapter.load()) {
                    loadDevicesDelay = DEVICE_LOAD_RETRY_INTERVAL;
                    break;
                }

                // May have a valid runner after load. In which case we just had to wait for
                // bus setup and devices to load. So we can keep using it.
                if (runner == null) {
                    virtualMachine.board.reset();
                    virtualMachine.board.initialize();
                    virtualMachine.board.setRunning(true);

                    runner = new ComputerVirtualMachineRunner(virtualMachine);
                }

                setRunState(RunState.RUNNING);

                // Only start running next tick. This gives loaded devices one tick to do async
                // initialization. This is used by RAM to restore data from disk, for example.
                break;
            case RUNNING:
                if (!virtualMachine.board.isRunning()) {
                    stopRunnerAndResetVM();
                    break;
                }

                runner.tick();
                chunk.markDirty();
                break;
        }
    }

    @Override
    public void remove() {
        super.remove();

        // Unload only suspends, but we want to do a full clean-up when we get
        // destroyed, so stuff inside us can delete out-of-nbt persisted data.
        virtualMachine.vmAdapter.unload();
    }

    @Override
    public CompoundNBT getUpdateTag() {
        final CompoundNBT result = super.getUpdateTag();

        result.put(TERMINAL_NBT_TAG_NAME, NBTSerialization.serialize(terminal));
        result.putInt(BUS_STATE_NBT_TAG_NAME, busState.ordinal());
        result.putInt(RUN_STATE_NBT_TAG_NAME, runState.ordinal());

        return result;
    }

    @Override
    public void handleUpdateTag(final BlockState state, final CompoundNBT tag) {
        super.handleUpdateTag(state, tag);

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_NBT_TAG_NAME), terminal);
        busState = AbstractDeviceBusController.BusState.values()[tag.getInt(BUS_STATE_NBT_TAG_NAME)];
        runState = RunState.values()[tag.getInt(RUN_STATE_NBT_TAG_NAME)];
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound = super.write(compound);

        joinVirtualMachine();

        compound.put(TERMINAL_NBT_TAG_NAME, NBTSerialization.serialize(terminal));

        compound.put(BUS_ELEMENT_NBT_TAG_NAME, NBTSerialization.serialize(busElement));
        compound.put(VIRTUAL_MACHINE_NBT_TAG_NAME, NBTSerialization.serialize(virtualMachine));

        if (runner != null) {
            compound.put(RUNNER_NBT_TAG_NAME, NBTSerialization.serialize(runner));
        } else {
            NBTUtils.putEnum(compound, RUN_STATE_NBT_TAG_NAME, runState);
        }

        compound.put(Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME, itemHandler.serializeNBT());

        return compound;
    }

    @Override
    public void read(final BlockState state, final CompoundNBT compound) {
        super.read(state, compound);

        joinVirtualMachine();

        NBTSerialization.deserialize(compound.getCompound(TERMINAL_NBT_TAG_NAME), terminal);

        if (compound.contains(BUS_ELEMENT_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(compound.getCompound(BUS_ELEMENT_NBT_TAG_NAME), busElement);
        }

        if (compound.contains(VIRTUAL_MACHINE_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(compound.getCompound(VIRTUAL_MACHINE_NBT_TAG_NAME), virtualMachine);
        }

        if (compound.contains(RUNNER_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            runner = new ComputerVirtualMachineRunner(virtualMachine);
            NBTSerialization.deserialize(compound.getCompound(RUNNER_NBT_TAG_NAME), runner);
            runState = RunState.LOADING_DEVICES;
        } else {
            runState = NBTUtils.getEnum(compound, RUN_STATE_NBT_TAG_NAME, RunState.class);
            if (runState == null) {
                runState = RunState.STOPPED;
            } else if (runState == RunState.RUNNING) {
                runState = RunState.LOADING_DEVICES;
            }
        }

        if (compound.contains(Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            itemHandler.deserializeNBT(compound.getCompound(Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME));
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void loadClient() {
        super.loadClient();

        terminal.setDisplayOnly(true);
    }

    @Override
    protected void loadServer() {
        super.loadServer();

        busElement.initialize();
        virtualMachine.rtc.setWorld(getWorld());
    }

    @Override
    protected void unloadServer() {
        super.unloadServer();

        joinVirtualMachine();
        virtualMachine.vmAdapter.suspend();

        busController.dispose();
        busElement.dispose();
    }

    ///////////////////////////////////////////////////////////////////

    private void setBusState(final AbstractDeviceBusController.BusState value) {
        if (value == busState) {
            return;
        }

        busState = value;

        final ComputerBusStateMessage message = new ComputerBusStateMessage(this);
        Network.sendToClientsTrackingChunk(message, chunk);
    }

    private void setRunState(final RunState value) {
        if (value == runState) {
            return;
        }

        runState = value;

        // This method can be called from disposal logic, so if we are disposed quickly enough
        // chunk may not be initialized yet. Avoid resulting NRE in network logic.
        if (chunk != null) {
            final ComputerRunStateMessage message = new ComputerRunStateMessage(this);
            Network.sendToClientsTrackingChunk(message, chunk);
        }
    }

    private void stopRunnerAndResetVM() {
        joinVirtualMachine();
        runner = null;
        setRunState(RunState.STOPPED);

        virtualMachine.reset();
    }

    private void joinVirtualMachine() {
        if (runner != null) {
            try {
                runner.join();
            } catch (final Throwable e) {
                LOGGER.error(e);
                runner = null;
            }
        }
    }

    private OptionalLong getDefaultDeviceAddress(final VMDevice wrapper, final MemoryMappedDevice device) {
        if (device instanceof PhysicalMemory) {
            return OptionalLong.empty();
        }

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            final Collection<ItemDeviceInfo> devices = itemHandler.getBusElement().getDeviceGroup(slot);
            for (final ItemDeviceInfo info : devices) {
                if (Objects.equals(info.device, wrapper)) {
                    return OptionalLong.of(ITEM_DEVICE_BASE_ADDRESS + slot * ITEM_DEVICE_STRIDE);
                }
            }
        }

        return OptionalLong.empty();
    }

    private void loadProgramFile(final InputStream stream) throws Throwable {
        loadProgramFile(stream, 0);
    }

    private void loadProgramFile(final InputStream stream, final int offset) throws Throwable {
        MemoryMaps.store(virtualMachine.board.getMemoryMap(), 0x80000000L + offset, stream);
    }

    ///////////////////////////////////////////////////////////////////

    private final class BusController extends AbstractDeviceBusController {
        private BusController() {
            super(busElement);
        }

        @Override
        protected void onBeforeScan() {
            if (runState == RunState.RUNNING) {
                runState = RunState.LOADING_DEVICES;
            }

            virtualMachine.rpcAdapter.pause();
        }

        @Override
        protected void onAfterDeviceScan(final boolean didDevicesChange) {
            virtualMachine.rpcAdapter.resume(didDevicesChange);
        }

        @Override
        protected void onDevicesAdded(final Collection<Device> devices) {
            virtualMachine.vmAdapter.addDevices(devices);
        }

        @Override
        protected void onDevicesRemoved(final Collection<Device> devices) {
            virtualMachine.vmAdapter.removeDevices(devices);
        }
    }

    private final class BusElement extends TileEntityDeviceBusElement {
        public BusElement() {
            super(ComputerTileEntity.this);
        }

        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return super.getNeighbors().map(neighbors -> {
                final ArrayList<LazyOptional<DeviceBusElement>> list = new ArrayList<>(neighbors);
                list.add(LazyOptional.of(itemHandler::getBusElement));
                return list;
            });
        }

        @Override
        public boolean canConnectToSide(@Nullable final Direction direction) {
            return getBlockState().get(ComputerBlock.HORIZONTAL_FACING) != direction;
        }
    }

    private static final class ComputerVirtualMachine extends VirtualMachine {
        private static final int UART_INTERRUPT = 0x3;
        private static final int VFS_INTERRUPT = 0x4;

        @Serialized public UART16550A uart;
        @Serialized public VirtIOFileSystemDevice vfs;

        public ComputerVirtualMachine(final DeviceBusController busController) {
            super(busController);

            final VMContext context = vmAdapter.getGlobalContext();
            uart = new UART16550A();
            context.getInterruptAllocator().claimInterrupt(UART_INTERRUPT).ifPresent(interrupt ->
                    uart.getInterrupt().set(interrupt, context.getInterruptController()));
            context.getMemoryRangeAllocator().claimMemoryRange(uart);
            board.setStandardOutputDevice(uart);

            vfs = new VirtIOFileSystemDevice(context.getMemoryMap(), "scripts", new HostFileSystem());
            context.getInterruptAllocator().claimInterrupt(VFS_INTERRUPT).ifPresent(interrupt ->
                    vfs.getInterrupt().set(interrupt, context.getInterruptController()));
            context.getMemoryRangeAllocator().claimMemoryRange(vfs);
        }
    }

    private final class ComputerVirtualMachineRunner extends VirtualMachineRunner {
        // Thread-local buffers for lock-free read/writes in inner loop.
        private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
        private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);

        private boolean firedResumeEvent;
        @Serialized private boolean firedInitializationEvent;

        public ComputerVirtualMachineRunner(final VirtualMachine virtualMachine) {
            super(virtualMachine.board);
        }

        @Override
        public void tick() {
            if (!firedResumeEvent) {
                firedResumeEvent = true;
                virtualMachine.vmAdapter.fireLifecycleEvent(VMDeviceLifecycleEventType.RESUME_RUNNING);
            }

            virtualMachine.rpcAdapter.tick();

            super.tick();
        }

        @Override
        protected void handleBeforeRun() {
            if (!firedInitializationEvent) {
                firedInitializationEvent = true;
                virtualMachine.vmAdapter.fireLifecycleEvent(VMDeviceLifecycleEventType.INITIALIZE);

                try {
                    loadProgramFile(Buildroot.getFirmware());
                    loadProgramFile(Buildroot.getLinuxImage(), 0x200000);
                } catch (final Throwable e) {
                    LOGGER.error(e);
                }
            }

            int value;
            while ((value = terminal.readInput()) != -1) {
                inputBuffer.enqueue((byte) value);
            }
        }

        @Override
        protected void step(final int cyclesPerStep) {
            while (!inputBuffer.isEmpty() && virtualMachine.uart.canPutByte()) {
                virtualMachine.uart.putByte(inputBuffer.dequeueByte());
            }
            virtualMachine.uart.flush();

            int value;
            while ((value = virtualMachine.uart.read()) != -1) {
                outputBuffer.enqueue((byte) value);
            }

            virtualMachine.rpcAdapter.step(cyclesPerStep);
        }

        @Override
        protected void handleAfterRun() {
            final ByteBuffer output = ByteBuffer.allocate(outputBuffer.size());
            while (!outputBuffer.isEmpty()) {
                output.put(outputBuffer.dequeueByte());
            }

            output.flip();
            if (output.hasRemaining()) {
                terminal.putOutput(output);

                output.flip();

                final TerminalBlockOutputMessage message = new TerminalBlockOutputMessage(ComputerTileEntity.this, output);
                Network.sendToClientsTrackingChunk(message, chunk);
            }
        }
    }
}
