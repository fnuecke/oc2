/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.device;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.sedna.api.Interrupt;
import li.cil.sedna.api.device.InterruptSource;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.device.Resettable;
import li.cil.sedna.api.device.Steppable;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.devicetree.provider.VirtIOProvider;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class GuestTestPortDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice {
    public static final String PORT_NAME = "oc2.test.0";

    static {
        DeviceTreeRegistry.putProvider(SteppablePort.class, new VirtIOProvider());
    }

    // --------------------------------------------------------------------- //

    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();

    @Nullable
    private SteppablePort port;
    @Nullable
    private GuestTestChannel channel;

    // --------------------------------------------------------------------- //

    public GuestTestPortDevice(final ItemStack identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    /**
     * The host end of the port, once the device has been mounted into a running machine.
     */
    @Nullable
    public GuestTestChannel getChannel() {
        return channel;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(Constants.PAGE_SIZE)) {
            return VMDeviceLoadResult.fail();
        }

        final VirtIOConsoleDevice console = new VirtIOConsoleDevice(context.getMemoryMap(), PORT_NAME);
        final GuestTestChannel newChannel = new GuestTestChannel(console.getPort(0));
        port = new SteppablePort(console, newChannel);

        if (!address.claim(context.getDeviceRangeAllocator(), port)) {
            return VMDeviceLoadResult.fail();
        }

        if (!interrupt.claim(context)) {
            return VMDeviceLoadResult.fail();
        }
        console.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());

        channel = newChannel;

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        port = null;
    }

    @Override
    public void dispose() {
        channel = null;
        address.clear();
        interrupt.clear();
    }

    // --------------------------------------------------------------------- //

    private static final class SteppablePort implements MemoryMappedDevice, InterruptSource, Resettable, Steppable {
        private final VirtIOConsoleDevice device;
        private final GuestTestChannel channel;

        SteppablePort(final VirtIOConsoleDevice device, final GuestTestChannel channel) {
            this.device = device;
            this.channel = channel;
        }

        @Override
        public void step(final int cycles) {
            channel.step();
        }

        @Override
        public int getLength() {
            return device.getLength();
        }

        @Override
        public int getSupportedSizes() {
            return device.getSupportedSizes();
        }

        @Override
        public long load(final int offset, final int sizeLog2) {
            return device.load(offset, sizeLog2);
        }

        @Override
        public void store(final int offset, final long value, final int sizeLog2) {
            device.store(offset, value, sizeLog2);
        }

        @Override
        public Iterable<Interrupt> getInterrupts() {
            return device.getInterrupts();
        }

        @Override
        public void reset() {
            device.reset();
        }
    }
}
