package li.cil.oc2.common.tile;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.OpenComputers;
import li.cil.oc2.client.gui.terminal.Terminal;
import li.cil.oc2.common.ServerScheduler;
import li.cil.oc2.common.bus.TileEntityDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.TerminalBlockOutputMessage;
import li.cil.oc2.common.tile.computer.VirtualMachine;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.vm.VirtualMachineRunner;
import li.cil.oc2.serialization.BlobStorage;
import li.cil.oc2.serialization.NBTSerialization;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;
import li.cil.sedna.fs.HostFileSystem;
import li.cil.sedna.memory.PhysicalMemoryInputStream;
import li.cil.sedna.memory.PhysicalMemoryOutputStream;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

public final class ComputerTileEntity extends AbstractTileEntity implements ITickableTileEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEVICES_NBT_TAG_NAME = "devices";
    private static final String BUS_ELEMENT_NBT_TAG_NAME = "busElement";
    private static final String BUS_STATE_NBT_TAG_NAME = "busState";
    private static final String TERMINAL_NBT_TAG_NAME = "terminal";
    private static final String VIRTUAL_MACHINE_NBT_TAG_NAME = "virtualMachine";
    private static final String RUNNER_NBT_TAG_NAME = "runner";
    private static final String RUN_STATE_NBT_TAG_NAME = "runState";

    private static final int DEVICE_LOAD_RETRY_INTERVAL = 10 * 20; // In ticks.

    private enum RunState {
        STOPPED,
        LOADING_DEVICES,
        RUNNING,
    }

    private Chunk chunk;
    private final TileEntityDeviceBusController busController;
    private TileEntityDeviceBusController.State busState;
    private RunState runState;
    private int loadDevicesDelay;
    @Nullable private BlobStorage.JobHandle ramJobHandle;

    // Serialized data

    private final TileEntityDeviceBusElement busElement;
    private final Terminal terminal;
    private final VirtualMachine virtualMachine;
    private ConsoleRunner runner;

    private PhysicalMemory ram;
    private UUID ramBlobHandle;
    private VirtIOBlockDevice hdd;

    public ComputerTileEntity() {
        super(OpenComputers.COMPUTER_TILE_ENTITY.get());

        busElement = new TileEntityDeviceBusElement(this);
        busController = new BusController();
        busState = TileEntityDeviceBusController.State.SCAN_PENDING;
        runState = RunState.STOPPED;

        terminal = new Terminal();
        virtualMachine = new VirtualMachine(busController);

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

        runState = RunState.LOADING_DEVICES;
        loadDevicesDelay = 0;
    }

    public void stop() {
        if (runState == RunState.STOPPED) {
            return;
        }

        if (runState == RunState.LOADING_DEVICES) {
            runState = RunState.STOPPED;
            return;
        }

        stopRunnerAndUnloadDevices();
    }

    public boolean isRunning() {
        return runner != null;
    }

    @Override
    public void tick() {
        final World world = getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        busState = busController.scan();
        if (busState != TileEntityDeviceBusController.State.READY) {
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

                runState = RunState.RUNNING;

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
    protected void initializeClient() {
        super.initializeClient();
        terminal.setDisplayOnly(true);
    }

    @Override
    protected void initializeServer() {
        super.initializeServer();

        ServerScheduler.schedule(() -> chunk = Objects.requireNonNull(getWorld()).getChunkAt(getPos()));
    }

    @Override
    protected void disposeServer() {
        super.disposeServer();

        busElement.dispose();
        stopRunnerAndUnloadDevices();
    }

    @Override
    public CompoundNBT getUpdateTag() {
        final CompoundNBT result = super.getUpdateTag();

        result.put(TERMINAL_NBT_TAG_NAME, NBTSerialization.serialize(terminal));
        result.putInt(BUS_STATE_NBT_TAG_NAME, busState.ordinal());

        return result;
    }

    @Override
    public void handleUpdateTag(final BlockState state,final CompoundNBT tag) {
        super.handleUpdateTag(state, tag);

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_NBT_TAG_NAME), terminal);
        busState = TileEntityDeviceBusController.State.values()[tag.getInt(BUS_STATE_NBT_TAG_NAME)];
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

        if (compound.contains(RUNNER_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            runner = new ConsoleRunner(virtualMachine);
            NBTSerialization.deserialize(compound.getCompound(RUNNER_NBT_TAG_NAME), runner);
            runState = RunState.LOADING_DEVICES;
        } else {
            runState = NBTUtils.getEnum(compound, RUN_STATE_NBT_TAG_NAME, RunState.class);
            if (runState == RunState.RUNNING) {
                runState = RunState.LOADING_DEVICES;
            }
        }

        // TODO Read item NBTs, generate item based devices.

        if (compound.contains(DEVICES_NBT_TAG_NAME, NBTTagIds.TAG_LIST)) {
            readDevices(compound.getList(DEVICES_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND));
        }
    }

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

        if (hdd != null) {
            final CompoundNBT hddNbt = new CompoundNBT();
            hddNbt.put("hdd", NBTSerialization.serialize(hdd));
            list.add(hddNbt);
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

        final CompoundNBT hddNbt = list.getCompound(1);
        if (hddNbt.contains("hdd", NBTTagIds.TAG_COMPOUND)) {
            if (hdd == null) {
                hdd = new VirtIOBlockDevice(virtualMachine.board.getMemoryMap());
                hdd.getInterrupt().set(0x3, virtualMachine.board.getInterruptController());
                virtualMachine.board.addDevice(hdd);
            }

            NBTSerialization.deserialize(hddNbt.getCompound("hdd"), hdd);
        }
    }

    private boolean loadDevices() {
        // TODO Load devices generated from items.

        try {
            if (ram == null) {
                final int RAM_SIZE = 24 * 1024 * 1024;
                ram = Memory.create(RAM_SIZE);
                virtualMachine.board.addDevice(0x80000000L, ram);
            }

            if (ramBlobHandle != null) {
                ramJobHandle = BlobStorage.JobHandle.combine(ramJobHandle,
                        BlobStorage.submitLoad(ramBlobHandle, new PhysicalMemoryOutputStream(ram)));
            }

            if (hdd == null) {
                hdd = new VirtIOBlockDevice(virtualMachine.board.getMemoryMap());
                hdd.getInterrupt().set(0x3, virtualMachine.board.getInterruptController());
                virtualMachine.board.addDevice(hdd);
            }

            final ByteBufferBlockDevice blockDevice = ByteBufferBlockDevice.createFromStream(Buildroot.getRootFilesystem(), true);
            hdd.setBlockDevice(blockDevice);

            final VirtIOFileSystemDevice vfs = new VirtIOFileSystemDevice(virtualMachine.board.getMemoryMap(), "scripts", new HostFileSystem());
            vfs.getInterrupt().set(0x4, virtualMachine.board.getInterruptController());
            virtualMachine.board.addDevice(vfs);
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        return true;
    }

    private void unloadDevices() {
        // TODO Unload devices generated from items.

        if (ram != null) {
            virtualMachine.board.removeDevice(ram);
            ram = null;
            BlobStorage.freeHandle(ramBlobHandle);
        }

        if (hdd != null) {
            virtualMachine.board.removeDevice(hdd);
            try {
                hdd.close();
            } catch (final IOException e) {
                LOGGER.error(e);
            }

            hdd = null;
        }
    }

    private void stopRunnerAndUnloadDevices() {
        joinVirtualMachine();
        runner = null;
        runState = RunState.STOPPED;

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

    private class BusController extends TileEntityDeviceBusController {
        private BusController() {
            super(ComputerTileEntity.this);
        }

        @Override
        protected void onDevicesInvalid() {
            virtualMachine.rpcAdapter.pause();
        }

        @Override
        protected void onDevicesValid() {
            virtualMachine.rpcAdapter.resume();
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
                final TerminalBlockOutputMessage message = new TerminalBlockOutputMessage(ComputerTileEntity.this, output);
                Network.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
            }
        }
    }
}
