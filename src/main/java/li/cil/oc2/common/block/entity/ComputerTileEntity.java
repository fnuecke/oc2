package li.cil.oc2.common.block.entity;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.Constants;
import li.cil.oc2.OpenComputers;
import li.cil.oc2.api.bus.Device;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.TerminalBlockOutputMessage;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.VirtualMachineRunner;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;
import li.cil.sedna.fs.HostFileSystem;
import li.cil.sedna.memory.PhysicalMemoryInputStream;
import li.cil.sedna.memory.PhysicalMemoryOutputStream;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class ComputerTileEntity extends AbstractTileEntity implements ITickableTileEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final String DEVICES_NBT_TAG_NAME = "devices";
    private static final String BUS_ELEMENT_NBT_TAG_NAME = "busElement";
    private static final String BUS_STATE_NBT_TAG_NAME = "busState";
    private static final String TERMINAL_NBT_TAG_NAME = "terminal";
    private static final String VIRTUAL_MACHINE_NBT_TAG_NAME = "virtualMachine";
    private static final String VFS_NBT_TAG_NAME = "vfs";
    private static final String RUNNER_NBT_TAG_NAME = "runner";
    private static final String RUN_STATE_NBT_TAG_NAME = "runState";

    private static final int DEVICE_LOAD_RETRY_INTERVAL = 10 * 20; // In ticks.

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
    @Nullable private BlobStorage.JobHandle ramJobHandle;

    ///////////////////////////////////////////////////////////////////

    private final TileEntityDeviceBusElement busElement;
    private final Terminal terminal;
    private final VirtualMachine virtualMachine;
    private final VirtIOFileSystemDevice vfs;
    private ConsoleRunner runner;

    private PhysicalMemory ram;
    private UUID ramBlobHandle;

    private final ItemStackHandler itemHandler = new ItemStackHandler();

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntity() {
        super(OpenComputers.COMPUTER_TILE_ENTITY.get());

        busElement = new BusElement();
        busController = new BusController();
        busState = AbstractDeviceBusController.BusState.SCAN_PENDING;
        runState = RunState.STOPPED;

        terminal = new Terminal();
        virtualMachine = new VirtualMachine(busController);

        vfs = new VirtIOFileSystemDevice(virtualMachine.board.getMemoryMap(), "scripts", new HostFileSystem());
        vfs.getInterrupt().set(virtualMachine.vmAdapter.claimInterrupt(), virtualMachine.board.getInterruptController());
        virtualMachine.board.addDevice(vfs);

        setCapabilityIfAbsent(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY, busElement);
        setCapabilityIfAbsent(Capabilities.DEVICE_BUS_CONTROLLER_CAPABILITY, busController);
    }

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

        stopRunnerAndUnloadDevices();
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
        if (side == getBlockState().get(ComputerBlock.HORIZONTAL_FACING)) {
            return LazyOptional.empty();
        }

        return super.getCapability(capability, side);
    }

    @Override
    public void tick() {
        final World world = getWorld();
        if (world == null || world.isRemote()) {
            return;
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

                if (!loadDevices()) {
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
                    stopRunnerAndUnloadDevices();
                    break;
                }

                runner.tick();
                chunk.markDirty();
                break;
        }
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
        if (ramJobHandle != null) {
            ramJobHandle.await();
            ramJobHandle = null;
        }

        compound.put(TERMINAL_NBT_TAG_NAME, NBTSerialization.serialize(terminal));

        compound.put(BUS_ELEMENT_NBT_TAG_NAME, NBTSerialization.serialize(busElement));
        compound.put(VIRTUAL_MACHINE_NBT_TAG_NAME, NBTSerialization.serialize(virtualMachine));
        compound.put(VFS_NBT_TAG_NAME, NBTSerialization.serialize(vfs));

        if (runner != null) {
            compound.put(RUNNER_NBT_TAG_NAME, NBTSerialization.serialize(runner));
        } else {
            NBTUtils.putEnum(compound, RUN_STATE_NBT_TAG_NAME, runState);
        }

        final ListNBT devicesNbt = new ListNBT();
        writeDevices(devicesNbt);
        compound.put(DEVICES_NBT_TAG_NAME, devicesNbt);

        return compound;
    }

    @Override
    public void read(final BlockState state, final CompoundNBT compound) {
        super.read(state, compound);

        joinVirtualMachine();
        if (ramJobHandle != null) {
            ramJobHandle.await();
            ramJobHandle = null;
        }

        NBTSerialization.deserialize(compound.getCompound(TERMINAL_NBT_TAG_NAME), terminal);

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

        // TODO Read item NBTs, generate item based devices.

        if (compound.contains(DEVICES_NBT_TAG_NAME, NBTTagIds.TAG_LIST)) {
            readDevices(compound.getList(DEVICES_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND));
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

        busElement.initialize();
        virtualMachine.rtc.setWorld(requireNonNull(getWorld()));
        ServerScheduler.schedule(() -> chunk = requireNonNull(getWorld()).getChunkAt(getPos()));
    }

    @Override
    protected void disposeServer() {
        super.disposeServer();

        busElement.dispose();
        stopRunnerAndUnloadDevices();
    }

    ///////////////////////////////////////////////////////////////////

    private void writeDevices(final ListNBT list) {
        // TODO Apply to devices generated from items.

        if (ram != null) {
            final CompoundNBT ramNbt = new CompoundNBT();
            ramBlobHandle = BlobStorage.validateHandle(ramBlobHandle);
            ramNbt.putUniqueId("ram", ramBlobHandle);
            list.add(ramNbt);

            ramJobHandle = BlobStorage.JobHandle.combine(ramJobHandle,
                    BlobStorage.submitSave(ramBlobHandle, new PhysicalMemoryInputStream(ram)));
        } else {
            list.add(new CompoundNBT());
        }
    }

    private void readDevices(final ListNBT list) {
        // TODO Apply to devices generated from items.

        final CompoundNBT ramNbt = list.getCompound(0);
        if (ramNbt.hasUniqueId("ram")) {
            ramBlobHandle = ramNbt.getUniqueId("ram");
        }
    }

    private boolean loadDevices() {
        // TODO Load devices generated from items.

        if (ram == null) {
            final int RAM_SIZE = 24 * Constants.MEGABYTE;
            ram = Memory.create(RAM_SIZE);
            virtualMachine.board.addDevice(0x80000000L, ram);
        }

        if (ramBlobHandle != null) {
            ramJobHandle = BlobStorage.JobHandle.combine(ramJobHandle,
                    BlobStorage.submitLoad(ramBlobHandle, new PhysicalMemoryOutputStream(ram)));
        }

        return virtualMachine.vmAdapter.load();
    }

    private void unloadDevices() {
        // TODO Unload devices generated from items.

        if (ram != null) {
            virtualMachine.board.removeDevice(ram);
            ram = null;
            BlobStorage.freeHandle(ramBlobHandle);
        }

        virtualMachine.vmAdapter.unload();
    }

    private void setBusState(final AbstractDeviceBusController.BusState value) {
        if (value == busState) {
            return;
        }

        busState = value;

        Network.sendToClientsTrackingChunk(new ComputerBusStateMessage(this), chunk);
    }

    private void setRunState(final RunState value) {
        if (value == runState) {
            return;
        }

        runState = value;

        Network.sendToClientsTrackingChunk(new ComputerRunStateMessage(this), chunk);
    }

    private void stopRunnerAndUnloadDevices() {
        joinVirtualMachine();
        runner = null;
        setRunState(RunState.STOPPED);

        unloadDevices();
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

    private static void loadProgramFile(final PhysicalMemory memory, final InputStream stream) throws Throwable {
        loadProgramFile(memory, stream, 0);
    }

    private static void loadProgramFile(final PhysicalMemory memory, final InputStream stream, final int offset) throws Throwable {
        final BufferedInputStream bis = new BufferedInputStream(stream);
        for (int address = 0, value = bis.read(); value != -1; value = bis.read(), address++) {
            memory.store(offset + address, (byte) value, Sizes.SIZE_8_LOG2);
        }
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
        protected boolean canConnectToSide(final Direction direction) {
            return getBlockState().get(ComputerBlock.HORIZONTAL_FACING) != direction;
        }
    }

    private final class ConsoleRunner extends VirtualMachineRunner {
        // Thread-local buffers for lock-free read/writes in inner loop.
        private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
        private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);

        @Serialized private boolean didInitialization;

        public ConsoleRunner(final VirtualMachine virtualMachine) {
            super(virtualMachine.board);
        }

        @Override
        public void tick() {
            // TODO Tick devices that need it synchronously.
            if (ramJobHandle != null) {
                ramJobHandle.await();
                ramJobHandle = null;
            }

            virtualMachine.rpcAdapter.tick();

            super.tick();
        }

        @Override
        protected void handleBeforeRun() {
            if (!didInitialization) {
                didInitialization = true;
                try {
                    // TODO Initialize devices that need it asynchronously.

                    loadProgramFile(ram, Buildroot.getFirmware());
                    loadProgramFile(ram, Buildroot.getLinuxImage(), 0x200000);
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
                Network.sendToClientsTrackingChunk(
                        new TerminalBlockOutputMessage(ComputerTileEntity.this, output), chunk);
            }
        }
    }
}
