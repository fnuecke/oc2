package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;

public final class CommonVirtualMachine extends VirtualMachine {
    private static final int UART_INTERRUPT = 0x4;
    private static final int VFS_INTERRUPT = 0x5;

    ///////////////////////////////////////////////////////////////////

    @Serialized public UART16550A uart;
    @Serialized public VirtIOFileSystemDevice vfs;

    ///////////////////////////////////////////////////////////////////

    public CommonVirtualMachine(final DeviceBusController busController) {
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
