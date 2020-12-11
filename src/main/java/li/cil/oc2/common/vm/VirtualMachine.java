package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.common.bus.RPCAdapter;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.riscv.R5Board;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class VirtualMachine {
    private static final Logger LOGGER = LogManager.getLogger();

    // We report a clock rate to the VM such that for the VM it looks as though
    // passes as much faster as MC time passes faster than real time.
    public static final int REPORTED_CPU_FREQUENCY = 700_000;
    public static final int ACTUAL_CPU_FREQUENCY = REPORTED_CPU_FREQUENCY * 72;

    public final MinecraftRealTimeCounter rtc = new MinecraftRealTimeCounter();

    @Serialized public R5Board board;
    @Serialized public VirtualMachineDeviceBusAdapter vmAdapter;
    @Serialized public UART16550A uart;
    @Serialized public VirtIOBlockDevice hdd;
    @Serialized public VirtIOConsoleDevice deviceBusSerialDevice;
    @Serialized public RPCAdapter rpcAdapter;

    public VirtualMachine(final DeviceBusController busController) {
        board = new R5Board();

        board.getCpu().setFrequency(REPORTED_CPU_FREQUENCY);

        vmAdapter = new VirtualMachineDeviceBusAdapter(board.getMemoryMap(), board.getInterruptController());

        uart = new UART16550A();
        uart.getInterrupt().set(vmAdapter.claimInterrupt(), board.getInterruptController());
        board.addDevice(uart);

        hdd = new VirtIOBlockDevice(board.getMemoryMap());
        hdd.getInterrupt().set(vmAdapter.claimInterrupt(), board.getInterruptController());
        board.addDevice(hdd);

        final ByteBufferBlockDevice blockDevice;
        try {
            blockDevice = ByteBufferBlockDevice.createFromStream(Buildroot.getRootFilesystem(), true);
            hdd.setBlockDevice(blockDevice);
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        final GoldfishRTC rtc = new GoldfishRTC(this.rtc);
        rtc.getInterrupt().set(vmAdapter.claimInterrupt(), board.getInterruptController());
        board.addDevice(rtc);

        deviceBusSerialDevice = new VirtIOConsoleDevice(board.getMemoryMap());
        deviceBusSerialDevice.getInterrupt().set(vmAdapter.claimInterrupt(), board.getInterruptController());
        board.addDevice(deviceBusSerialDevice);

        rpcAdapter = new RPCAdapter(busController, deviceBusSerialDevice);

        board.setBootArguments("root=/dev/vda ro");
        board.setStandardOutputDevice(uart);
    }

    public void reset() {
        board.reset();
        rpcAdapter.reset();
    }
}
