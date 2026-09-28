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
    private static final long MLAPI_WINDOW_ADDRESS = 0x03000000L;
    private static final int MLAPI_INTERRUPT = 6;
    private static final String BOOT_ARGUMENTS = "root=/dev/vda rw uio_pdrv_genirq.of_id=oc2,mlapi";

    @Serialized
    private final R5Board board;
    @Serialized
    private final BuiltinDevices builtinDevices;
    @Serialized
    private final RPCDeviceBusAdapter hlapiAdapter;
    @Serialized
    private final IODeviceBusAdapter mlapiAdapter;
    @Serialized
    private final IODeviceBusWindow mlapiWindow;

    // --------------------------------------------------------------------- //

    public R5Architecture(final Config config) {
        this(new R5Board(), config);
    }

    private R5Architecture(final R5Board board, final Config config) {
        super(board, board.getCpu(), config);
        this.board = board;
        this.builtinDevices = new BuiltinDevices(getContext());
        builtinDevices.rtcMinecraft.setGameTimeSource(config.gameTimeProvider());
        this.hlapiAdapter = new RPCDeviceBusAdapter(builtinDevices.getRpcPort(), builtinDevices.getBlobPort(), builtinDevices.getEventPort(), config.consumeEnergy());
        this.mlapiAdapter = new IODeviceBusAdapter(config.consumeEnergy());
        this.mlapiWindow = new IODeviceBusWindow(mlapiAdapter);
        if (!getContext().getMemoryRangeAllocator().claimMemoryRange(MLAPI_WINDOW_ADDRESS, mlapiWindow)) {
            throw new IllegalStateException("Mid-level API window does not fit the memory map.");
        }
        if (!getContext().getInterruptAllocator().claimInterrupt(MLAPI_INTERRUPT)) {
            throw new IllegalStateException("Mid-level API interrupt is already claimed.");
        }
        mlapiAdapter.getInterrupt().set(MLAPI_INTERRUPT, getContext().getInterruptController());

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
        hlapiAdapter.step(cycles);
        mlapiAdapter.step();
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
    public void unmountAllDevices() {
        super.unmountAllDevices();
        hlapiAdapter.unmountDevices();
        mlapiAdapter.unmountDevices();
    }

    // --------------------------------------------------------------------- //

    @Override
    public void handleAfterDeviceScan(final DeviceBusController controller) {
        super.handleAfterDeviceScan(controller);
        hlapiAdapter.rebuild(controller);
        mlapiAdapter.rebuild(controller);
    }

    @Override
    public void mountDynamicDevices() {
        hlapiAdapter.mountDevices();
        mlapiAdapter.mountDevices();
    }

    @Override
    public void tickDynamicDevices() {
        hlapiAdapter.tick();
        mlapiAdapter.tick();
    }

    @Override
    protected void resetDynamicDevices() {
        hlapiAdapter.reset();
        mlapiAdapter.reset();
    }
}
