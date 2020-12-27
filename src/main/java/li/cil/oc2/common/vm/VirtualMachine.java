package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.vm.InterruptAllocator;
import li.cil.oc2.api.bus.device.vm.MemoryRangeAllocator;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.common.bus.RPCAdapter;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.rtc.SystemTimeRealTimeCounter;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.riscv.R5Board;

public class VirtualMachine {
    public static final int CPU_FREQUENCY = 33_000_000;

    public static final int RTC_HOST_INTERRUPT = 0x1;
    public static final int RTC_MINECRAFT_INTERRUPT = 0x2;
    public static final int RPC_INTERRUPT = 0x3;

    ///////////////////////////////////////////////////////////////////

    public final MinecraftRealTimeCounter rtcMinecraft = new MinecraftRealTimeCounter();

    ///////////////////////////////////////////////////////////////////

    @Serialized public R5Board board;
    @Serialized public VirtualMachineDeviceBusAdapter vmAdapter;
    @Serialized public VirtIOConsoleDevice deviceBusSerialDevice;
    @Serialized public RPCAdapter rpcAdapter;

    ///////////////////////////////////////////////////////////////////

    public VirtualMachine(final DeviceBusController busController) {
        board = new R5Board();

        board.getCpu().setFrequency(CPU_FREQUENCY);

        vmAdapter = new VirtualMachineDeviceBusAdapter(board);
        final VMContext context = vmAdapter.getGlobalContext();

        final MemoryRangeAllocator memoryRangeAllocator = context.getMemoryRangeAllocator();
        final InterruptAllocator interruptAllocator = context.getInterruptAllocator();
        final InterruptController interruptController = context.getInterruptController();

        final GoldfishRTC rtcHost = new GoldfishRTC(SystemTimeRealTimeCounter.get());
        interruptAllocator.claimInterrupt(RTC_HOST_INTERRUPT).ifPresent(interrupt ->
                rtcHost.getInterrupt().set(interrupt, interruptController));
        memoryRangeAllocator.claimMemoryRange(rtcHost);

        final GoldfishRTC rtcMinecraft = new GoldfishRTC(this.rtcMinecraft);
        interruptAllocator.claimInterrupt(RTC_MINECRAFT_INTERRUPT).ifPresent(interrupt ->
                rtcMinecraft.getInterrupt().set(interrupt, interruptController));
        memoryRangeAllocator.claimMemoryRange(rtcMinecraft);

        deviceBusSerialDevice = new VirtIOConsoleDevice(board.getMemoryMap());
        interruptAllocator.claimInterrupt(RPC_INTERRUPT).ifPresent(interrupt ->
                deviceBusSerialDevice.getInterrupt().set(interrupt, interruptController));
        memoryRangeAllocator.claimMemoryRange(deviceBusSerialDevice);

        rpcAdapter = new RPCAdapter(busController, deviceBusSerialDevice);

        board.setBootArguments("root=/dev/vda rw");
    }

    ///////////////////////////////////////////////////////////////////

    public void reset() {
        board.reset();
        rpcAdapter.reset();
        vmAdapter.unload();
    }
}
