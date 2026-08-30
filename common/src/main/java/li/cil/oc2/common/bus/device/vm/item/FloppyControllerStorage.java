/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.cpm.Cpm;
import li.cil.sedna.device.disk.WD1793;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.io.IOException;

public final class FloppyControllerStorage implements MappedStorage {
    private static final int UNIT = 0;

    private final WD1793 controller = new WD1793();
    @Nullable
    private BlockDevice medium;

    // --------------------------------------------------------------------- //

    public FloppyControllerStorage() {
        controller.setUnitCount(1);
    }

    // --------------------------------------------------------------------- //

    @Override
    public MemoryMappedDevice getDevice() {
        return controller;
    }

    @Override
    public VMDeviceLoadResult claim(final VMContext context, final OptionalAddress address, final OptionalInterrupt interrupt) {
        if (!address.claim(context.getDeviceRangeAllocator(), controller)) {
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_DEVICE_DOES_NOT_FIT));
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void setBlockDevice(@Nullable final BlockDevice block) {
        if (block == null) {
            controller.removeDisk(UNIT);
        } else {
            controller.setDisk(UNIT, block, Cpm.SIDES, Cpm.TRACKS, Cpm.SECTORS_PER_TRACK, Cpm.SECTOR_SIZE);
        }

        medium = block;
    }

    @Override
    public void close() throws IOException {
        controller.removeDisk(UNIT);

        if (medium != null) {
            medium.close();
            medium = null;
        }
    }
}
