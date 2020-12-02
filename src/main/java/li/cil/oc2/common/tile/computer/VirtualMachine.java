package li.cil.oc2.common.tile.computer;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.common.bus.RPCAdapter;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.rtc.SystemTimeRealTimeCounter;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.riscv.R5Board;

public final class VirtualMachine {
    @Serialized public R5Board board;
    @Serialized public UART16550A uart;
    @Serialized public VirtIOConsoleDevice deviceBusSerialDevice;
    @Serialized public RPCAdapter rpcAdapter;

    public VirtualMachine(final DeviceBusController busController) {
        board = new R5Board();

        uart = new UART16550A();
        uart.getInterrupt().set(0x1, board.getInterruptController());
        board.addDevice(uart);

        final GoldfishRTC rtc = new GoldfishRTC(SystemTimeRealTimeCounter.get());
        rtc.getInterrupt().set(0x2, board.getInterruptController());
        board.addDevice(rtc);

        deviceBusSerialDevice = new VirtIOConsoleDevice(board.getMemoryMap());
        deviceBusSerialDevice.getInterrupt().set(0x3, board.getInterruptController());
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
