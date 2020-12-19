package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.vm.InterruptAllocator;
import li.cil.oc2.api.bus.device.vm.MemoryRangeAllocator;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.common.bus.RPCAdapter;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.riscv.R5Board;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class VirtualMachine {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    // We report a clock rate to the VM such that for the VM it looks as though
    // passes as much faster as MC time passes faster than real time.
    public static final int REPORTED_CPU_FREQUENCY = 700_000;
    public static final int ACTUAL_CPU_FREQUENCY = REPORTED_CPU_FREQUENCY * 72;

    private static final int UART_INTERRUPT = 0x1;
    private static final int RTC_INTERRUPT = 0x2;
    private static final int RPC_INTERRUPT = 0x3;

    private static final ByteBufferBlockDevice ROOT_FS;

    static {
        ByteBufferBlockDevice rootfs;
        try {
            rootfs = ByteBufferBlockDevice.createFromStream(Buildroot.getRootFilesystem(), true);
        } catch (final IOException e) {
            LOGGER.error(e);
            rootfs = ByteBufferBlockDevice.create(0, true);
        }
        ROOT_FS = rootfs;
    }

    ///////////////////////////////////////////////////////////////////

    public final MinecraftRealTimeCounter rtc = new MinecraftRealTimeCounter();

    ///////////////////////////////////////////////////////////////////

    @Serialized public R5Board board;
    @Serialized public VirtualMachineDeviceBusAdapter vmAdapter;
    @Serialized public UART16550A uart;
    @Serialized public VirtIOConsoleDevice deviceBusSerialDevice;
    @Serialized public RPCAdapter rpcAdapter;

    ///////////////////////////////////////////////////////////////////

    public VirtualMachine(final DeviceBusController busController) {
        board = new R5Board();

        board.getCpu().setFrequency(REPORTED_CPU_FREQUENCY);

        vmAdapter = new VirtualMachineDeviceBusAdapter(board);
        final VMContext context = vmAdapter.getGlobalContext();

        final MemoryRangeAllocator memoryRangeAllocator = context.getMemoryRangeAllocator();
        final InterruptAllocator interruptAllocator = context.getInterruptAllocator();
        final InterruptController interruptController = context.getInterruptController();

        uart = new UART16550A();
        interruptAllocator.claimInterrupt(UART_INTERRUPT).ifPresent(interrupt ->
                uart.getInterrupt().set(interrupt, interruptController));
        memoryRangeAllocator.claimMemoryRange(uart);

        final GoldfishRTC rtc = new GoldfishRTC(this.rtc);
        interruptAllocator.claimInterrupt(RTC_INTERRUPT).ifPresent(interrupt ->
                rtc.getInterrupt().set(interrupt, interruptController));
        memoryRangeAllocator.claimMemoryRange(rtc);

        deviceBusSerialDevice = new VirtIOConsoleDevice(board.getMemoryMap());
        interruptAllocator.claimInterrupt(RPC_INTERRUPT).ifPresent(interrupt ->
                deviceBusSerialDevice.getInterrupt().set(interrupt, interruptController));
        memoryRangeAllocator.claimMemoryRange(deviceBusSerialDevice);

        rpcAdapter = new RPCAdapter(busController, deviceBusSerialDevice);

        board.setBootArguments("root=/dev/vda rw");
        board.setStandardOutputDevice(uart);
    }

    public void reset() {
        board.reset();
        rpcAdapter.reset();
        vmAdapter.unload();
    }
}
