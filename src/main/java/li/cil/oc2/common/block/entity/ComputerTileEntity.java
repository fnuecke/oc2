package li.cil.oc2.common.block.entity;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.AttributeProviderBlockEntity;
import alexiil.mc.lib.attributes.Simulation;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.Constants;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.AttributeProviderDevice;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDeviceLifecycleEventType;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.DeviceItemStackHandler;
import li.cil.oc2.common.init.Items;
import li.cil.oc2.common.init.TileEntities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.TerminalBlockOutputMessage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.*;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.VirtualMachineRunner;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;
import li.cil.sedna.fs.HostFileSystem;
import li.cil.sedna.memory.MemoryMaps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public final class ComputerTileEntity extends AbstractTileEntity implements Tickable, AttributeProviderBlockEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final String BUS_ELEMENT_NBT_TAG_NAME = "busElement";
    private static final String BUS_STATE_NBT_TAG_NAME = "busState";
    private static final String TERMINAL_NBT_TAG_NAME = "terminal";
    private static final String VIRTUAL_MACHINE_NBT_TAG_NAME = "virtualMachine";
    private static final String VFS_NBT_TAG_NAME = "vfs";
    private static final String RUNNER_NBT_TAG_NAME = "runner";
    private static final String RUN_STATE_NBT_TAG_NAME = "runState";
    private static final String ITEMS_NBT_TAG_NAME = "items";

    private static final int DEVICE_LOAD_RETRY_INTERVAL = 10 * 20; // In ticks.
    private static final int VFS_INTERRUPT = 0x4;

    ///////////////////////////////////////////////////////////////////

    public enum RunState {
        STOPPED,
        LOADING_DEVICES,
        RUNNING,
    }

    ///////////////////////////////////////////////////////////////////

    private final Runnable onWorldUnloaded = this::onWorldUnloaded;
    private Chunk chunk;
    private final AbstractDeviceBusController busController;
    private AbstractDeviceBusController.BusState busState;
    private RunState runState;
    private int loadDevicesDelay;

    ///////////////////////////////////////////////////////////////////

    private final TileEntityDeviceBusElement busElement;
    private final Terminal terminal;
    private final VirtualMachine virtualMachine;
    private final VirtIOFileSystemDevice vfs;
    private ConsoleRunner runner;

    private final DeviceItemStackHandler itemHandler = new DeviceItemStackHandler(8);

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntity() {
        super(TileEntities.COMPUTER_TILE_ENTITY);

        busElement = new BusElement();
        busController = new BusController();
        busState = AbstractDeviceBusController.BusState.SCAN_PENDING;
        runState = RunState.STOPPED;

        terminal = new Terminal();
        virtualMachine = new VirtualMachine(busController);

        final VMContext context = virtualMachine.vmAdapter.getGlobalContext();
        vfs = new VirtIOFileSystemDevice(context.getMemoryMap(), "scripts", new HostFileSystem());
        context.getInterruptAllocator().claimInterrupt(VFS_INTERRUPT).ifPresent(interrupt ->
                vfs.getInterrupt().set(interrupt, context.getInterruptController()));
        context.getMemoryRangeAllocator().claimMemoryRange(vfs);

        itemHandler.setInvStack(0, new ItemStack(Items.RAM_8M_ITEM), Simulation.ACTION);
        itemHandler.setInvStack(1, new ItemStack(Items.RAM_8M_ITEM), Simulation.ACTION);
        itemHandler.setInvStack(2, new ItemStack(Items.RAM_8M_ITEM), Simulation.ACTION);

        final ItemStack hdd = new ItemStack(Items.HDD_ITEM);
        ItemStackUtils.getOrCreateModDataTag(hdd).putString(Constants.HDD_BASE_NBT_TAG_NAME, "linux");
        itemHandler.setInvStack(4, hdd, Simulation.ACTION);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void addAllAttributes(final AttributeList<?> attributeList) {
        if (attributeList.getSearchDirection() == getCachedState().get(ComputerBlock.FACING)) {
            if (attributeList.attribute == Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY ||
                attributeList.attribute == Capabilities.DEVICE_BUS_CONTROLLER_CAPABILITY) {
                return;
            }
        }

        attributeList.offer(busElement);
        attributeList.offer(busController);

        final Direction localSide = HorizontalBlockUtils.toLocal(getCachedState(), attributeList.getSearchDirection());

        for (final Device device : busController.getDevices()) {
            if (device instanceof AttributeProviderDevice) {
                ((AttributeProviderDevice) device).addAllAttributes(attributeList, localSide);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    public Terminal getTerminal() {
        return terminal;
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

    @Environment(EnvType.CLIENT)
    public void setRunStateClient(final RunState value) {
        final World world = getWorld();
        if (world != null && world.isClient()) {
            runState = value;
        }
    }

    @Environment(EnvType.CLIENT)
    public void setBusStateClient(final AbstractDeviceBusController.BusState value) {
        final World world = getWorld();
        if (world != null && world.isClient()) {
            busState = value;
        }
    }

    @Override
    public void tick() {
        final World world = getWorld();
        if (world == null || world.isClient()) {
            return;
        }

        if (chunk == null) {
            chunk = world.getChunk(getPos());
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

                    runner = new ConsoleRunner(virtualMachine);
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
                chunk.setShouldSave(true);
                break;
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();

        // Regular dispose only suspends, but we want to do a full unload when we get
        // destroyed, so stuff inside us can delete out-of-nbt persisted data.
        virtualMachine.vmAdapter.unload();
    }

    @Override
    public CompoundTag toInitialChunkDataTag() {
        final CompoundTag result = super.toInitialChunkDataTag();

        result.put(TERMINAL_NBT_TAG_NAME, NBTSerialization.serialize(terminal));
        result.putInt(BUS_STATE_NBT_TAG_NAME, busState.ordinal());
        result.putInt(RUN_STATE_NBT_TAG_NAME, runState.ordinal());

        return result;
    }

    public void handleUpdateTag(final BlockState state, final CompoundTag tag) {
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_NBT_TAG_NAME), terminal);
        busState = AbstractDeviceBusController.BusState.values()[tag.getInt(BUS_STATE_NBT_TAG_NAME)];
        runState = RunState.values()[tag.getInt(RUN_STATE_NBT_TAG_NAME)];
    }

    @Override
    public CompoundTag toTag(CompoundTag compound) {
        compound = super.toTag(compound);

        joinVirtualMachine();

        compound.put(TERMINAL_NBT_TAG_NAME, NBTSerialization.serialize(terminal));

        compound.put(BUS_ELEMENT_NBT_TAG_NAME, NBTSerialization.serialize(busElement));
        compound.put(VIRTUAL_MACHINE_NBT_TAG_NAME, NBTSerialization.serialize(virtualMachine));
        compound.put(VFS_NBT_TAG_NAME, NBTSerialization.serialize(vfs));

        if (runner != null) {
            compound.put(RUNNER_NBT_TAG_NAME, NBTSerialization.serialize(runner));
        } else {
            NBTUtils.putEnum(compound, RUN_STATE_NBT_TAG_NAME, runState);
        }

        compound.put(ITEMS_NBT_TAG_NAME, itemHandler.toTag());

        return compound;
    }

    @Override
    public void fromTag(final BlockState state, final CompoundTag compound) {
        super.fromTag(state, compound);

        joinVirtualMachine();

        NBTSerialization.deserialize(compound.getCompound(TERMINAL_NBT_TAG_NAME), terminal);

        if (compound.contains(BUS_STATE_NBT_TAG_NAME, NBTTagIds.TAG_INT)) {
            busState = AbstractDeviceBusController.BusState.values()[compound.getInt(BUS_STATE_NBT_TAG_NAME)];
        }
        if (compound.contains(RUN_STATE_NBT_TAG_NAME, NBTTagIds.TAG_INT)) {
            runState = RunState.values()[compound.getInt(RUN_STATE_NBT_TAG_NAME)];
        }

        if (compound.contains(BUS_ELEMENT_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(compound.getCompound(BUS_ELEMENT_NBT_TAG_NAME), busElement);
        }

        if (compound.contains(VIRTUAL_MACHINE_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(compound.getCompound(VIRTUAL_MACHINE_NBT_TAG_NAME), virtualMachine);
        }

        if (compound.contains(VFS_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(compound.getCompound(VFS_NBT_TAG_NAME), vfs);
        }

        if (compound.contains(RUNNER_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            runner = new ConsoleRunner(virtualMachine);
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

        if (compound.contains(ITEMS_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            for (int i = 0; i < itemHandler.getSlotCount(); i++) {
                itemHandler.setInvStack(i, ItemStack.EMPTY, Simulation.ACTION);
            }
            itemHandler.fromTag(compound.getCompound(ITEMS_NBT_TAG_NAME));
        }

        if (getWorld() != null && getWorld().isClient()) {
            handleUpdateTag(state, compound);
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void initializeClient() {
        super.initializeClient();

        terminal.setDisplayOnly(true);
    }

    @Override
    protected void initializeServer() {
        super.initializeServer();

        final World world = requireNonNull(getWorld());
        ServerScheduler.scheduleOnUnload(world, onWorldUnloaded);

        busElement.initialize();
        virtualMachine.rtc.setWorld(world);
    }

    @Override
    protected void disposeServer() {
        super.disposeServer();

        ServerScheduler.removeOnUnload(getWorld(), onWorldUnloaded);

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

        if (world instanceof ServerWorld) {
            final ComputerBusStateMessage message = new ComputerBusStateMessage(this);
            Network.sendToClientsTrackingChunk(message, (ServerWorld) world, chunkPos);
        }
    }

    private void setRunState(final RunState value) {
        if (value == runState) {
            return;
        }

        runState = value;

        // This method can be called from disposal logic, so if we are disposed quickly enough
        // chunk may not be initialized yet. Avoid resulting NRE in network logic.
        if (world instanceof ServerWorld) {
            final ComputerRunStateMessage message = new ComputerRunStateMessage(this);
            Network.sendToClientsTrackingChunk(message, (ServerWorld) world, chunkPos);
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

    private void loadProgramFile(final InputStream stream) throws Throwable {
        loadProgramFile(stream, 0);
    }

    private void loadProgramFile(final InputStream stream, final int offset) throws Throwable {
        MemoryMaps.store(virtualMachine.board.getMemoryMap(), 0x80000000L + offset, stream);
    }

    private void onWorldUnloaded() {
        disposeServer();
    }

    ///////////////////////////////////////////////////////////////////

    private final class BusController extends AbstractDeviceBusController {
        private BusController() {
            super(busElement);
        }

        @Override
        protected void onDevicesInvalid() {
            if (runState == RunState.RUNNING) {
                runState = RunState.LOADING_DEVICES;
            }

            virtualMachine.rpcAdapter.pause();
        }

        @Override
        protected void onDevicesValid(final boolean didDevicesChange) {
            virtualMachine.rpcAdapter.resume(didDevicesChange);
        }

        @Override
        protected void onDevicesAdded(final Set<Device> devices) {
            virtualMachine.vmAdapter.addDevices(devices);
        }

        @Override
        protected void onDevicesRemoved(final Set<Device> devices) {
            virtualMachine.vmAdapter.removeDevices(devices);
        }
    }

    private final class BusElement extends TileEntityDeviceBusElement {
        public BusElement() {
            super(ComputerTileEntity.this);
        }

        @Override
        public Optional<Collection<DeviceBusElement>> getNeighbors() {
            return super.getNeighbors().map(neighbors -> {
                final ArrayList<DeviceBusElement> list = new ArrayList<>(neighbors);
                list.add(itemHandler.getBusElement());
                return list;
            });
        }

        @Override
        protected boolean canConnectToSide(final Direction direction) {
            return getCachedState().get(ComputerBlock.FACING) != direction;
        }
    }

    private final class ConsoleRunner extends VirtualMachineRunner {
        // Thread-local buffers for lock-free read/writes in inner loop.
        private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
        private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);

        private boolean firedResumeEvent;
        @Serialized private boolean firedInitializationEvent;

        public ConsoleRunner(final VirtualMachine virtualMachine) {
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

                if (world instanceof ServerWorld) {
                    final TerminalBlockOutputMessage message = new TerminalBlockOutputMessage(ComputerTileEntity.this, output);
                    Network.sendToClientsTrackingChunk(message, (ServerWorld) world, chunkPos);
                }
            }
        }
    }
}
