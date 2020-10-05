package li.cil.oc2.common.tile;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.OpenComputers;
import li.cil.oc2.client.gui.terminal.Terminal;
import li.cil.oc2.common.blobs.BlobStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.TerminalBlockOutputMessage;
import li.cil.oc2.common.sandbox.Allocator;
import li.cil.oc2.common.vm.VirtualMachineRunner;
import li.cil.oc2.serialization.NBTSerialization;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.memory.PhysicalMemoryInputStream;
import li.cil.sedna.memory.PhysicalMemoryOutputStream;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
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

public final class ComputerTileEntity extends TileEntity implements ITickableTileEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    @Serialized private final Terminal terminal = new Terminal();
    private VirtualMachine virtualMachine;
    private VirtualMachineRunner runner;

    @Serialized private UUID firmwareBlobHandle;
    @Serialized private UUID ramBlobHandle;
    private BlobStorage.JobHandle blobStorageJobHandle;

    private Chunk chunk;

    public ComputerTileEntity() {
        super(OpenComputers.COMPUTER_TILE_ENTITY.get());
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void start() {
        startVirtualMachine();
    }

    public void stop() {
        disposeVirtualMachine();
    }

    public boolean isRunning() {
        return runner != null;
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote()) {
            return;
        }

        if (chunk == null) {
            chunk = Objects.requireNonNull(getWorld()).getChunkAt(getPos());
        }

        if (blobStorageJobHandle != null) {
            blobStorageJobHandle.await();
            blobStorageJobHandle = null;
        }

        if (runner != null) {
            runner.tick();
            chunk.markDirty();
        }
    }

    @Override
    public void remove() {
        super.remove();
        disposeVirtualMachine();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        disposeVirtualMachine();
    }

    @Override
    public CompoundNBT getUpdateTag() {
        final CompoundNBT result = super.getUpdateTag();

        result.put("terminal", NBTSerialization.serialize(terminal));
        return result;
    }

    @Override
    public void handleUpdateTag(final CompoundNBT tag) {
        super.handleUpdateTag(tag);
        NBTSerialization.deserialize(tag.getCompound("terminal"), terminal);
    }

    @Override
    public void read(final CompoundNBT compound) {
        super.read(compound);

        joinVirtualMachine();

        if (blobStorageJobHandle != null) {
            blobStorageJobHandle.await();
            blobStorageJobHandle = null;
        }

        NBTSerialization.deserialize(compound, this);

        if (compound.contains("virtualMachine")) {
            virtualMachine = VirtualMachine.allocate();

            if (virtualMachine != null) {
                NBTSerialization.deserialize(compound.getCompound("virtualMachine"), virtualMachine);

                firmwareBlobHandle = BlobStorage.validateHandle(firmwareBlobHandle);
                ramBlobHandle = BlobStorage.validateHandle(ramBlobHandle);

                blobStorageJobHandle = BlobStorage.JobHandle.combine(blobStorageJobHandle,
                        BlobStorage.submitLoad(firmwareBlobHandle, new PhysicalMemoryOutputStream(virtualMachine.firmware)));
                blobStorageJobHandle = BlobStorage.JobHandle.combine(blobStorageJobHandle,
                        BlobStorage.submitLoad(ramBlobHandle, new PhysicalMemoryOutputStream(virtualMachine.ram)));

                if (compound.contains("runner")) {
                    runner = new ConsoleRunner(virtualMachine);
                    NBTSerialization.deserialize(compound.getCompound("runner"), runner);
                }
            } else {
                // TODO Let user know VM could not be created because of memory constraints.
            }
        }
    }

    @Override
    public CompoundNBT write(final CompoundNBT compound) {
        final CompoundNBT result = super.write(compound);

        joinVirtualMachine();

        if (blobStorageJobHandle != null) {
            blobStorageJobHandle.await();
            blobStorageJobHandle = null;
        }

        if (virtualMachine != null) {
            compound.put("virtualMachine", NBTSerialization.serialize(virtualMachine));

            firmwareBlobHandle = BlobStorage.validateHandle(firmwareBlobHandle);
            ramBlobHandle = BlobStorage.validateHandle(ramBlobHandle);

            blobStorageJobHandle = BlobStorage.JobHandle.combine(blobStorageJobHandle,
                    BlobStorage.submitSave(firmwareBlobHandle, new PhysicalMemoryInputStream(virtualMachine.firmware)));
            blobStorageJobHandle = BlobStorage.JobHandle.combine(blobStorageJobHandle,
                    BlobStorage.submitSave(ramBlobHandle, new PhysicalMemoryInputStream(virtualMachine.ram)));

            if (runner != null) {
                compound.put("runner", NBTSerialization.serialize(runner));
            }
        } else {
            compound.remove("virtualMachine");
        }

        // Do this last so that blob handles have been updated.
        NBTSerialization.serialize(compound, this);

        return result;
    }

    private void startVirtualMachine() {
        if (Objects.requireNonNull(getWorld()).isRemote()) {
            return;
        }

        if (runner != null) {
            return;
        }

        if (virtualMachine == null) {
            virtualMachine = VirtualMachine.allocate();
        }

        if (virtualMachine == null) {
            // TODO Let user know VM could not be created because of memory constraints.
            return;
        }

        virtualMachine.board.reset();
        runner = new ConsoleRunner(virtualMachine);
    }

    private void disposeVirtualMachine() {
        joinVirtualMachine();
        runner = null;
        if (virtualMachine != null) {
            virtualMachine.dispose();
            virtualMachine = null;
        }

        BlobStorage.freeHandle(firmwareBlobHandle);
        BlobStorage.freeHandle(ramBlobHandle);
    }

    private void joinVirtualMachine() {
        if (runner != null) {
            try {
                runner.join();
            } catch (final Throwable e) {
                LOGGER.warn(e);
                runner = null;
            }
        }
    }

    private static void loadProgramFile(final PhysicalMemory memory, final InputStream stream) throws Throwable {
        final BufferedInputStream bis = new BufferedInputStream(stream);
        for (int address = 0, value = bis.read(); value != -1; value = bis.read(), address++) {
            memory.store(address, (byte) value, Sizes.SIZE_8_LOG2);
        }
    }

    private static final class VirtualMachine {
        @Serialized R5Board board;
        @Serialized UART16550A uart;
        @Serialized VirtIOBlockDevice hdd;
        PhysicalMemory firmware;
        PhysicalMemory ram;

        UUID ramHandle;

        VirtualMachine(final UUID ramHandle, final BlockDevice blockDevice) {
            this.ramHandle = ramHandle;

            board = new R5Board();

            firmware = Memory.create(128 * 1024);
            ram = Memory.create(32 * 1024 * 1024);

            board.addDevice(0x80000000, firmware);
            board.addDevice(0x80000000 + 0x400000, ram);

            uart = new UART16550A();
            hdd = new VirtIOBlockDevice(board.getMemoryMap(), blockDevice);

            hdd.getInterrupt().set(0x1, board.getInterruptController());
            uart.getInterrupt().set(0x2, board.getInterruptController());

            board.addDevice(uart);
            board.addDevice(hdd);

            board.setBootargs("console=ttyS0 root=/dev/vda ro");
        }

        void dispose() {
            ram = null;
            Allocator.freeMemory(ramHandle);
        }

        @Nullable
        static VirtualMachine allocate() {
            final UUID ramHandle = Allocator.createHandle();
            if (Allocator.claimMemory(ramHandle, 32 * 1024 * 1024)) {
                try {
                    return new VirtualMachine(ramHandle, ByteBufferBlockDevice.createFromStream(Buildroot.getRootFilesystem(), true));
                } catch (final IOException e) {
                    LOGGER.error(e);
                    Allocator.freeMemory(ramHandle);
                }
            }

            return null;
        }
    }

    @Serialized
    private final class ConsoleRunner extends VirtualMachineRunner {
        // Thread-local buffers for lock-free read/writes in inner loop.
        private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
        private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);
        private boolean didLoadBinaries;

        public ConsoleRunner(final VirtualMachine virtualMachine) {
            super(virtualMachine.board);
        }

        @Override
        protected void handleBeforeRun() {
            if (!didLoadBinaries) {
                didLoadBinaries = true;
                try {
                    loadProgramFile(virtualMachine.firmware, Buildroot.getFirmware());
                    loadProgramFile(virtualMachine.ram, Buildroot.getLinuxImage());
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
        protected void step() {
            while (!inputBuffer.isEmpty() && virtualMachine.uart.canPutByte()) {
                virtualMachine.uart.putByte(inputBuffer.dequeueByte());
            }

            int value;
            while ((value = virtualMachine.uart.read()) != -1) {
                outputBuffer.enqueue((byte) value);
            }
        }

        @Override
        protected void handleAfterRun() {
            final ByteBuffer output = ByteBuffer.allocate(outputBuffer.size());
            while (!outputBuffer.isEmpty()) {
                output.put(outputBuffer.dequeueByte());
            }

            output.flip();
            terminal.putOutput(output);

            output.flip();
            final TerminalBlockOutputMessage message = new TerminalBlockOutputMessage(ComputerTileEntity.this, output);
            Network.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
        }
    }
}
