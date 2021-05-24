package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.rtc.SystemTimeRealTimeCounter;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;

public final class BuiltinDevices {
    public static final int RTC_HOST_INTERRUPT = 0x1;
    public static final int RTC_MINECRAFT_INTERRUPT = 0x2;
    public static final int RPC_INTERRUPT = 0x3;
    private static final int UART_INTERRUPT = 0x4;
    private static final int VFS_INTERRUPT = 0x5;

    ///////////////////////////////////////////////////////////////////

    public final MinecraftRealTimeCounter rtcMinecraft = new MinecraftRealTimeCounter();

    ///////////////////////////////////////////////////////////////////

    @Serialized public VirtIOConsoleDevice rpcSerialDevice;
    @Serialized public UART16550A uart;
    @Serialized public VirtIOFileSystemDevice vfs;

    ///////////////////////////////////////////////////////////////////

    public BuiltinDevices(final GlobalVMContext context) {
        final GoldfishRTC rtcHost = new GoldfishRTC(SystemTimeRealTimeCounter.get());
        if (!context.getInterruptAllocator().claimInterrupt(RTC_HOST_INTERRUPT)) throw new IllegalStateException();
        rtcHost.getInterrupt().set(RTC_HOST_INTERRUPT, context.getInterruptController());
        context.getMemoryRangeAllocator().claimMemoryRange(rtcHost);

        final GoldfishRTC rtcMinecraft = new GoldfishRTC(this.rtcMinecraft);
        if (!context.getInterruptAllocator().claimInterrupt(RTC_MINECRAFT_INTERRUPT)) throw new IllegalStateException();
        rtcMinecraft.getInterrupt().set(RTC_MINECRAFT_INTERRUPT, context.getInterruptController());
        context.getMemoryRangeAllocator().claimMemoryRange(rtcMinecraft);

        rpcSerialDevice = new VirtIOConsoleDevice(context.getMemoryMap());
        if (!context.getInterruptAllocator().claimInterrupt(RPC_INTERRUPT)) throw new IllegalStateException();
        rpcSerialDevice.getInterrupt().set(RPC_INTERRUPT, context.getInterruptController());
        context.getMemoryRangeAllocator().claimMemoryRange(rpcSerialDevice);

        uart = new UART16550A();
        if (!context.getInterruptAllocator().claimInterrupt(UART_INTERRUPT)) throw new IllegalStateException();
        uart.getInterrupt().set(UART_INTERRUPT, context.getInterruptController());
        context.getMemoryRangeAllocator().claimMemoryRange(uart);

        vfs = new VirtIOFileSystemDevice(context.getMemoryMap(), "builtin", FileSystems.getLayeredFileSystem());
        if (!context.getInterruptAllocator().claimInterrupt(VFS_INTERRUPT)) throw new IllegalStateException();
        vfs.getInterrupt().set(VFS_INTERRUPT, context.getInterruptController());
        context.getMemoryRangeAllocator().claimMemoryRange(vfs);
    }
}
