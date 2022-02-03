/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.api.Interrupt;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.rtc.SystemTimeRealTimeCounter;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;

import java.util.function.Function;

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
        initialize(context, new GoldfishRTC(SystemTimeRealTimeCounter.get()), RTC_HOST_INTERRUPT, GoldfishRTC::getInterrupt);
        initialize(context, new GoldfishRTC(this.rtcMinecraft), RTC_MINECRAFT_INTERRUPT, GoldfishRTC::getInterrupt);
        rpcSerialDevice = initialize(context, new VirtIOConsoleDevice(context.getMemoryMap()), RPC_INTERRUPT, VirtIOConsoleDevice::getInterrupt);
        uart = initialize(context, new UART16550A(), UART_INTERRUPT, UART16550A::getInterrupt);
        vfs = initialize(context, new VirtIOFileSystemDevice(context.getMemoryMap(), "builtin", FileSystems.getLayeredFileSystem()), VFS_INTERRUPT, VirtIOFileSystemDevice::getInterrupt);
    }

    ///////////////////////////////////////////////////////////////////

    private static <T extends MemoryMappedDevice> T initialize(final GlobalVMContext context, final T device, final int interrupt, final Function<T, Interrupt> interruptSupplier) {
        if (!context.getInterruptAllocator().claimInterrupt(interrupt)) throw new IllegalStateException();
        interruptSupplier.apply(device).set(interrupt, context.getInterruptController());
        context.getMemoryRangeAllocator().claimMemoryRange(device);
        return device;
    }
}
