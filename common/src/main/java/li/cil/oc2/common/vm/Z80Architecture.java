/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.common.Constants;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.api.device.serial.SerialDevice;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.cpm.Cpm;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.bus.DeviceEnumerator;
import li.cil.sedna.device.disk.WD1793;
import li.cil.sedna.device.flash.FlashMemoryDevice;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.memory.SimpleMemoryMap;
import li.cil.sedna.z80.BootRomLatch;
import li.cil.sedna.z80.Z80Board;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Z80Architecture extends AbstractArchitecture {
    private static final int BOOT_ROM_SIZE = 8 * Constants.KILOBYTE;
    private static final int ENUMERATOR_PORT = 0xE0;
    private static final int ROM_DRIVE_UNITS = 1;

    // --------------------------------------------------------------------- //

    @Serialized
    private final Z80Board board;
    @Serialized
    private final UART16550A uart;
    @Serialized
    private final WD1793 controller;
    @Serialized
    private final byte[] bootRom = new byte[BOOT_ROM_SIZE];

    private final transient MemoryMap bootRomMap;

    // --------------------------------------------------------------------- //

    public Z80Architecture() {
        this(new Z80Board());
    }

    private Z80Architecture(final Z80Board board) {
        super(board, board.getPortBus());
        this.board = board;
        this.uart = new UART16550A();
        this.controller = new WD1793();

        final ByteBuffer romData = ByteBuffer.wrap(bootRom).order(ByteOrder.LITTLE_ENDIAN);
        board.setBootRom(new FlashMemoryDevice(romData, true));

        final SimpleMemoryMap romMap = new SimpleMemoryMap();
        romMap.addDevice(0, new FlashMemoryDevice(romData, false));
        this.bootRomMap = romMap;

        mapPorts();
        insertRomDrive();
    }

    // --------------------------------------------------------------------- //

    @Override
    public ArchitectureType getType() {
        return ArchitectureType.Z80;
    }

    @Override
    public int getFrequency() {
        return board.getCpu().getFrequency();
    }

    @Override
    public SerialDevice getTerminalDevice() {
        return uart;
    }

    @Override
    public long getInstructionsRetired() {
        return board.getCpu().getCycles();
    }

    // --------------------------------------------------------------------- //

    @Override
    public void boot() {
        board.reset();
        board.setRunning(true);
    }

    @Override
    public void step(final int cycles) {
        board.step(cycles);
    }

    @Override
    public boolean isRunning() {
        return board.isRunning() && !board.isHalted();
    }

    @Override
    public void setRunning(final boolean value) {
        board.setRunning(value);
    }

    @Override
    public void reset() {
        board.reset();
    }

    @Override
    public void sendInitializingEvent() {
        sendLifecycleEvent(new VMInitializingEvent(bootRomMap, 0));
    }

    // --------------------------------------------------------------------- //

    private void mapPorts() {
        final DeviceEnumerator enumerator = new DeviceEnumerator(
            board.getPortMap(), board.getDevices(), board.getInterruptController());
        if (!board.addPortDevice(ENUMERATOR_PORT, enumerator)
            || board.addPortDevice(uart).isEmpty()
            || board.addPortDevice(controller).isEmpty()
            || board.addPortDevice(new BootRomLatch(board)).isEmpty()) {
            throw new IllegalStateException("Built-in devices do not fit the port space.");
        }
    }

    private void insertRomDrive() {
        final BlockDevice drive;
        try {
            drive = ByteBufferBlockDevice.createFromStream(Cpm.getFloppyImage(), true);
        } catch (final IOException e) {
            throw new IllegalStateException("Missing the built-in system disk.", e);
        }
        controller.setUnitCount(ROM_DRIVE_UNITS);
        controller.setDisk(0, drive, Cpm.SIDES, Cpm.TRACKS, Cpm.SECTORS_PER_TRACK, Cpm.SECTOR_SIZE);
    }
}
