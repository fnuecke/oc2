/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.io.IOException;

public final class VirtIOStorage implements MappedStorage {
    private static final ByteBufferBlockDevice EMPTY = ByteBufferBlockDevice.create(0, false);

    private final VirtIOBlockDevice device;

    // --------------------------------------------------------------------- //

    public VirtIOStorage(final VMContext context, final boolean readonly) {
        this.device = new VirtIOBlockDevice(context.getMemoryMap(), readonly, Constants.VIRTIO_BLOCK_QUEUE_SIZE);
    }

    // --------------------------------------------------------------------- //

    @Override
    public MemoryMappedDevice getDevice() {
        return device;
    }

    @Override
    public VMDeviceLoadResult claim(final VMContext context, final OptionalAddress address, final OptionalInterrupt interrupt) {
        if (!address.claim(context.getDeviceRangeAllocator(), device)) {
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_DEVICE_DOES_NOT_FIT));
        }

        if (!interrupt.claim(context)) {
            return VMDeviceLoadResult.fail();
        }

        device.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());

        return VMDeviceLoadResult.success();
    }

    @Override
    public void setBlockDevice(@Nullable final BlockDevice block) throws IOException {
        device.setBlock(block != null ? block : EMPTY);
    }

    @Override
    public void close() throws IOException {
        device.close();
    }
}
