package li.cil.oc2.common.tileentity;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLifecycleEventType;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusController;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.DeviceItemStackHandler;
import li.cil.oc2.common.container.TypedDeviceItemStackHandler;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerBootErrorMessage;
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
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

public final class ComputerTileEntity extends AbstractTileEntity implements ITickableTileEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    public static final String MEMORY_TAG_NAME = "memory";
    public static final String HARD_DRIVE_TAG_NAME = "hard_drive";
    public static final String FLASH_MEMORY_TAG_NAME = "flash_memory";
    public static final String CARD_TAG_NAME = "card";

    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String VIRTUAL_MACHINE_TAG_NAME = "virtualMachine";
    private static final String RUNNER_TAG_NAME = "runner";

    private static final String BUS_STATE_TAG_NAME = "busState";
    private static final String RUN_STATE_TAG_NAME = "runState";
    private static final String BOOT_ERROR_TAG_NAME = "bootError";

    private static final int DEVICE_LOAD_RETRY_INTERVAL = 10 * 20; // In ticks.

    private static final int MEMORY_SLOTS = 4;
    private static final int HARD_DRIVE_SLOTS = 4;
    private static final int FLASH_MEMORY_SLOTS = 1;
    private static final int CARD_SLOTS = 4;

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
    private ITextComponent bootError;
    private int loadDevicesDelay;
    private boolean hasAddedOwnDevices;
    private boolean isNeighborUpdateScheduled;

    ///////////////////////////////////////////////////////////////////

    private final DeviceItemStackHandler memoryItemHandler = new ComputerItemHandler(MEMORY_SLOTS, this::getDevices, DeviceTypes.MEMORY);
    private final DeviceItemStackHandler hardDriveItemHandler = new ComputerItemHandler(HARD_DRIVE_SLOTS, this::getDevices, DeviceTypes.HARD_DRIVE);
    private final DeviceItemStackHandler flashMemoryItemHandler = new ComputerItemHandler(FLASH_MEMORY_SLOTS, this::getDevices, DeviceTypes.FLASH_MEMORY);
    private final DeviceItemStackHandler cardItemHandler = new ComputerItemHandler(CARD_SLOTS, this::getDevices, DeviceTypes.CARD);

    private final IItemHandler itemHandlers = new CombinedInvWrapper(memoryItemHandler, hardDriveItemHandler, flashMemoryItemHandler, cardItemHandler);

    private final Terminal terminal = new Terminal();
    private final TileEntityDeviceBusElement busElement = new ComputerBusElement();
    private final ComputerVirtualMachine virtualMachine;
    private ComputerVirtualMachineRunner runner;

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntity() {
        super(TileEntities.COMPUTER_TILE_ENTITY.get());

        // We want to unload devices even on world unload to free global resources.
        setNeedsWorldUnloadEvent();

        busController = new ComputerBusController(busElement);
        busState = AbstractDeviceBusController.BusState.SCAN_PENDING;
        runState = RunState.STOPPED;

        virtualMachine = new ComputerVirtualMachine(busController);
        virtualMachine.vmAdapter.setDefaultAddressProvider(this::getDefaultDeviceAddress);
    }

    public Optional<IItemHandler> getItemHandler(final DeviceType deviceType) {
        if (deviceType == DeviceTypes.MEMORY) {
            return Optional.of(memoryItemHandler);
        } else if (deviceType == DeviceTypes.HARD_DRIVE) {
            return Optional.of(hardDriveItemHandler);
        } else if (deviceType == DeviceTypes.FLASH_MEMORY) {
            return Optional.of(flashMemoryItemHandler);
        } else if (deviceType == DeviceTypes.CARD) {
            return Optional.of(cardItemHandler);
        }
        return Optional.empty();
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void start() {
        if (runState == RunState.RUNNING) {
            return;
        }

        final World world = getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        setBootError(null);
        setRunState(RunState.LOADING_DEVICES);
        loadDevicesDelay = 0;
    }

    public void stop() {
        if (runState == RunState.STOPPED) {
            return;
        }

        final World world = getWorld();
        if (world == null || world.isRemote()) {
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

    @Nullable
    public ITextComponent getBootError() {
        switch (getBusState()) {
            case SCAN_PENDING:
            case INCOMPLETE:
                return new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_INCOMPLETE);
            case TOO_COMPLEX:
                return new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_TOO_COMPLEX);
            case MULTIPLE_CONTROLLERS:
                return new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS);
            case READY:
                switch (getRunState()) {
                    case STOPPED:
                    case LOADING_DEVICES:
                        return bootError;
                }
                break;
        }
        return null;
    }

    public void handleNeighborChanged() {
        busController.scheduleBusScan();
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

    @OnlyIn(Dist.CLIENT)
    public void setBootErrorClient(final ITextComponent value) {
        final World world = getWorld();
        if (world != null && world.isRemote()) {
            bootError = value;
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(final @NotNull Capability<T> capability, @Nullable final Direction side) {
        if (isRemoved()) {
            return LazyOptional.empty();
        }

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
    public void tick() {
        final World world = getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        if (chunk == null) {
            chunk = world.getChunkAt(getPos());
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

                final VMDeviceLoadResult loadResult = virtualMachine.vmAdapter.load();
                if (!loadResult.wasSuccessful()) {
                    if (loadResult.getErrorMessage() != null) {
                        setBootError(loadResult.getErrorMessage());
                    } else {
                        setBootError(new TranslationTextComponent(Constants.COMPUTER_BOOT_ERROR_UNKNOWN));
                    }
                    loadDevicesDelay = DEVICE_LOAD_RETRY_INTERVAL;
                    break;
                }

                // May have a valid runner after load. In which case we just had to wait for
                // bus setup and devices to load. So we can keep using it.
                if (runner == null) {
                    try {
                        virtualMachine.board.reset();
                        virtualMachine.board.initialize();
                        virtualMachine.board.setRunning(true);
                    } catch (final IllegalStateException e) {
                        // FDT did not fit into memory. Technically it's possible to run with
                        // a program that only uses registers. But not supporting that esoteric
                        // use-case loses out against avoiding people getting confused for having
                        // forgotten to add some RAM modules.
                        setBootError(new TranslationTextComponent(Constants.COMPUTER_BOOT_ERROR_NO_MEMORY));
                        setRunState(RunState.STOPPED);
                        return;
                    } catch (final MemoryAccessException e) {
                        LOGGER.error(e);
                        setBootError(new TranslationTextComponent(Constants.COMPUTER_BOOT_ERROR_UNKNOWN));
                        setRunState(RunState.STOPPED);
                        return;
                    }

                    runner = new ComputerVirtualMachineRunner(virtualMachine);
                }

                setRunState(RunState.RUNNING);

                // Only start running next tick. This gives loaded devices one tick to do async
                // initialization. This is used by devices to restore data from disk, for example.
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

        result.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        result.putInt(BUS_STATE_TAG_NAME, busState.ordinal());
        result.putInt(RUN_STATE_TAG_NAME, runState.ordinal());
        result.putString(BOOT_ERROR_TAG_NAME, ITextComponent.Serializer.toJson(bootError));

        return result;
    }

    @Override
    public void handleUpdateTag(final BlockState state, final CompoundNBT tag) {
        super.handleUpdateTag(state, tag);

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        busState = AbstractDeviceBusController.BusState.values()[tag.getInt(BUS_STATE_TAG_NAME)];
        runState = RunState.values()[tag.getInt(RUN_STATE_TAG_NAME)];
        bootError = ITextComponent.Serializer.getComponentFromJson(tag.getString(BOOT_ERROR_TAG_NAME));
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag = super.write(tag);

        joinVirtualMachine();
        runner.scheduleResumeEvent(); // Allow synchronizing to async device saves.

        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));

        tag.put(BUS_ELEMENT_TAG_NAME, NBTSerialization.serialize(busElement));
        tag.put(VIRTUAL_MACHINE_TAG_NAME, NBTSerialization.serialize(virtualMachine));

        if (runner != null) {
            tag.put(RUNNER_TAG_NAME, NBTSerialization.serialize(runner));
        } else {
            NBTUtils.putEnum(tag, RUN_STATE_TAG_NAME, runState);
        }

        final CompoundNBT items = new CompoundNBT();
        items.put(MEMORY_TAG_NAME, memoryItemHandler.serializeNBT());
        items.put(HARD_DRIVE_TAG_NAME, hardDriveItemHandler.serializeNBT());
        items.put(FLASH_MEMORY_TAG_NAME, flashMemoryItemHandler.serializeNBT());
        items.put(CARD_TAG_NAME, cardItemHandler.serializeNBT());
        tag.put(Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME, items);

        return tag;
    }

    @Override
    public void read(final BlockState state, final CompoundNBT tag) {
        super.read(state, tag);

        joinVirtualMachine();

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);

        if (tag.contains(BUS_ELEMENT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(tag.getCompound(BUS_ELEMENT_TAG_NAME), busElement);
        }

        if (tag.contains(VIRTUAL_MACHINE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(tag.getCompound(VIRTUAL_MACHINE_TAG_NAME), virtualMachine);
        }

        if (tag.contains(RUNNER_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            runner = new ComputerVirtualMachineRunner(virtualMachine);
            NBTSerialization.deserialize(tag.getCompound(RUNNER_TAG_NAME), runner);
            runState = RunState.LOADING_DEVICES;
        } else {
            runState = NBTUtils.getEnum(tag, RUN_STATE_TAG_NAME, RunState.class);
            if (runState == null) {
                runState = RunState.STOPPED;
            } else if (runState == RunState.RUNNING) {
                runState = RunState.LOADING_DEVICES;
            }
        }

        if (tag.contains(Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            final CompoundNBT items = tag.getCompound(Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME);
            memoryItemHandler.deserializeNBT(items.getCompound(MEMORY_TAG_NAME));
            hardDriveItemHandler.deserializeNBT(items.getCompound(HARD_DRIVE_TAG_NAME));
            flashMemoryItemHandler.deserializeNBT(items.getCompound(FLASH_MEMORY_TAG_NAME));
            cardItemHandler.deserializeNBT(items.getCompound(CARD_TAG_NAME));
        }
    }

    public boolean isEmpty() {
        for (int slot = 0; slot < itemHandlers.getSlots(); slot++) {
            if (!itemHandlers.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void exportDeviceDataToItemStacks() {
        memoryItemHandler.exportDeviceDataToItemStacks();
        hardDriveItemHandler.exportDeviceDataToItemStacks();
        flashMemoryItemHandler.exportDeviceDataToItemStacks();
        cardItemHandler.exportDeviceDataToItemStacks();
    }

    public void exportToItemStack(final ItemStack stack) {
        final CompoundNBT items = new CompoundNBT();
        items.put(MEMORY_TAG_NAME, memoryItemHandler.serializeNBT());
        items.put(HARD_DRIVE_TAG_NAME, hardDriveItemHandler.serializeNBT());
        items.put(FLASH_MEMORY_TAG_NAME, flashMemoryItemHandler.serializeNBT());
        items.put(CARD_TAG_NAME, cardItemHandler.serializeNBT());

        stack.getOrCreateChildTag(Constants.BLOCK_ENTITY_TAG_NAME_IN_ITEM)
                .put(Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME, items);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.ITEM_HANDLER, itemHandlers);
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
        virtualMachine.rtcMinecraft.setWorld(getWorld());
    }

    @Override
    protected void unloadServer() {
        super.unloadServer();

        joinVirtualMachine();
        virtualMachine.vmAdapter.suspend();

        busController.dispose();

        // This is necessary in case some other controller found us before our controller
        // did its scan, which can happen because the scan can happen with a delay.
        busElement.scheduleScan();
    }

    ///////////////////////////////////////////////////////////////////

    private List<ItemDeviceInfo> getDevices(final ItemStack stack) {
        return Devices.getDevices(this, stack);
    }

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

    private void setBootError(@Nullable final ITextComponent value) {
        if (Objects.equals(value, bootError)) {
            return;
        }

        bootError = value;
        final ComputerBootErrorMessage message = new ComputerBootErrorMessage(this);
        Network.sendToClientsTrackingChunk(message, chunk);
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

    private OptionalLong getDefaultDeviceAddress(final VMDevice wrapper) {
        long address = ITEM_DEVICE_BASE_ADDRESS;

        for (int slot = 0; slot < hardDriveItemHandler.getSlots(); slot++) {
            final Collection<ItemDeviceInfo> devices = hardDriveItemHandler.getBusElement().getDeviceGroup(slot);
            for (final ItemDeviceInfo info : devices) {
                if (Objects.equals(info.device, wrapper)) {
                    return OptionalLong.of(address);
                }
            }

            address += ITEM_DEVICE_STRIDE;
        }

        for (int slot = 0; slot < cardItemHandler.getSlots(); slot++) {
            final Collection<ItemDeviceInfo> devices = cardItemHandler.getBusElement().getDeviceGroup(slot);
            for (final ItemDeviceInfo info : devices) {
                if (Objects.equals(info.device, wrapper)) {
                    return OptionalLong.of(address);
                }
            }

            address += ITEM_DEVICE_STRIDE;
        }

        return OptionalLong.empty();
    }

    ///////////////////////////////////////////////////////////////////

    private final class ComputerItemHandler extends TypedDeviceItemStackHandler {
        public ComputerItemHandler(final int size, final Function<ItemStack, List<ItemDeviceInfo>> deviceLookup, final DeviceType deviceType) {
            super(size, deviceLookup, deviceType);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);
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

    private final class ComputerBusElement extends TileEntityDeviceBusElement {
        public ComputerBusElement() {
            super(ComputerTileEntity.this);
        }

        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return super.getNeighbors().map(neighbors -> {
                final ArrayList<LazyOptional<DeviceBusElement>> list = new ArrayList<>(neighbors);
                list.add(LazyOptional.of(flashMemoryItemHandler::getBusElement));
                list.add(LazyOptional.of(memoryItemHandler::getBusElement));
                list.add(LazyOptional.of(hardDriveItemHandler::getBusElement));
                list.add(LazyOptional.of(cardItemHandler::getBusElement));
                return list;
            });
        }

        @Override
        public boolean canScanContinueTowards(@Nullable final Direction direction) {
            return getBlockState().get(ComputerBlock.HORIZONTAL_FACING) != direction;
        }
    }

    private static final class ComputerVirtualMachine extends VirtualMachine {
        private static final int UART_INTERRUPT = 0x4;
        private static final int VFS_INTERRUPT = 0x5;

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

            vfs = new VirtIOFileSystemDevice(context.getMemoryMap(), "data", FileSystems.getLayeredFileSystem());
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

        public void scheduleResumeEvent() {
            firedResumeEvent = false;
        }

        @Override
        public void tick() {
            virtualMachine.rpcAdapter.tick();

            super.tick();
        }

        @Override
        protected void handleBeforeRun() {
            if (!firedInitializationEvent) {
                firedInitializationEvent = true;
                virtualMachine.vmAdapter.fireLifecycleEvent(VMDeviceLifecycleEventType.INITIALIZE);
            }

            if (!firedResumeEvent) {
                firedResumeEvent = true;
                virtualMachine.vmAdapter.fireLifecycleEvent(VMDeviceLifecycleEventType.RESUME_RUNNING);
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
