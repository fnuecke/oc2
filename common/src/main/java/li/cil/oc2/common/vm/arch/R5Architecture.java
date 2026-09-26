/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.arch;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.IODeviceBusAdapter;
import li.cil.oc2.common.bus.RPCDeviceBusAdapter;
import li.cil.oc2.common.vm.BuiltinDevices;
import li.cil.oc2.common.vm.DeviceLocation;
import li.cil.oc2.common.vm.device.IODeviceBusWindow;
import li.cil.sedna.api.device.serial.SerialDevice;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.riscv.R5Board;

import java.util.OptionalLong;

public final class R5Architecture extends AbstractArchitecture {
    private static final long ITEM_DEVICE_BASE_ADDRESS = 0x20000000L;
    private static final int ITEM_DEVICE_STRIDE = 0x1000;
    private static final long BUS_DEVICE_BASE_ADDRESS = 0x30000000L;
    private static final long IO_WINDOW_ADDRESS = 0x03000000L;
    private static final String BOOT_ARGUMENTS = "root=/dev/vda rw uio_pdrv_genirq.of_id=oc2,mlapi";

    @Serialized
    private final R5Board board;
    @Serialized
    private final BuiltinDevices builtinDevices;
    @Serialized
    private final RPCDeviceBusAdapter rpcAdapter;
    @Serialized
    private final IODeviceBusAdapter ioAdapter;
    @Serialized
    private final IODeviceBusWindow ioWindow;

    // --------------------------------------------------------------------- //

    public R5Architecture(final Config config) {
        this(new R5Board(), config);
    }

    private R5Architecture(final R5Board board, final Config config) {
        super(board, board.getCpu(), config);
        this.board = board;
        this.builtinDevices = new BuiltinDevices(getContext());
        builtinDevices.rtcMinecraft.setGameTimeSource(config.gameTimeProvider());
        this.rpcAdapter = new RPCDeviceBusAdapter(builtinDevices.getRpcPort(), builtinDevices.getBlobPort(), builtinDevices.getEventPort());
        this.ioAdapter = new IODeviceBusAdapter(config.runtime());
        this.ioWindow = new IODeviceBusWindow(ioAdapter);
        if (!getContext().getMemoryRangeAllocator().claimMemoryRange(IO_WINDOW_ADDRESS, ioWindow)) {
            throw new IllegalStateException("Mid-level API window does not fit the memory map.");
        }

        board.getCpu().setFrequency(li.cil.oc2.common.Config.riscvCycleBudgetPerSecond);
        board.setStandardOutputDevice(builtinDevices.uart);
        board.setFirmwareSize(Constants.FLASH_MEMORY_SIZE);
    }

    // --------------------------------------------------------------------- //

    @Override
    public ArchitectureType getType() {
        return ArchitectureType.RISCV;
    }

    @Override
    public int getFrequency() {
        return board.getCpu().getFrequency();
    }

    @Override
    public SerialDevice getTerminalDevice() {
        return builtinDevices.uart;
    }

    @Override
    public long getInstructionsRetired() {
        return board.getCpu().getInstructionsRetired();
    }

    @Override
    protected OptionalLong getDeviceAddress(final DeviceLocation location) {
        // Slotted in devices before bus devices so internal drives consistently show up as sda, sdb etc. on Linux.
        return switch (location.kind()) {
            case SLOT -> OptionalLong.of(ITEM_DEVICE_BASE_ADDRESS + (long) location.slot() * ITEM_DEVICE_STRIDE);
            case BUS -> OptionalLong.of(BUS_DEVICE_BASE_ADDRESS);
            case UNSPECIFIED -> OptionalLong.empty();
        };
    }

    // --------------------------------------------------------------------- //

    @Override
    public void boot() throws MemoryAccessException {
        board.reset();
        board.setBootArguments(BOOT_ARGUMENTS);
        board.initialize();
        board.setRunning(true);
    }

    @Override
    public void step(final int cycles) {
        board.step(cycles);
        rpcAdapter.step(cycles);
    }

    @Override
    public boolean isRunning() {
        return board.isRunning();
    }

    @Override
    public void setRunning(final boolean value) {
        board.setRunning(value);
    }

    @Override
    public boolean isRestarting() {
        return board.isRestarting();
    }

    @Override
    public void reset() {
        board.reset();
    }

    @Override
    public void sendInitializingEvent() {
        sendLifecycleEvent(new VMInitializingEvent(getContext().getMemoryMap(), board.getDefaultProgramStart()));
    }

    // --------------------------------------------------------------------- //

    @Override
    public void unmountDevices() {
        super.unmountDevices();
        rpcAdapter.unmountDevices();
    }

    @Override
    public void disposeDevices() {
        rpcAdapter.disposeDevices();
        super.disposeDevices();
    }

    // --------------------------------------------------------------------- //

    @Override
    public void handleBeforeDeviceScan() {
        rpcAdapter.pause();
    }

    @Override
    public void handleAfterDeviceScan(final DeviceBusController controller) {
        rpcAdapter.resume(controller);
        ioAdapter.rebuild(controller);
    }

    @Override
    public void startDevicesLayer() {
        rpcAdapter.mountDevices();
    }

    @Override
    public void tickDeviceLayer() {
        rpcAdapter.tick();
        ioAdapter.tick();
    }

    @Override
    protected void resetDeviceLayer() {
        rpcAdapter.reset();
        ioAdapter.reset();
    }
}
