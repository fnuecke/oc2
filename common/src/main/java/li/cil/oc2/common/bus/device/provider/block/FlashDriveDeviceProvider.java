/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.FlashDriveDevice;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FlashDriveDeviceProvider implements BlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        final BlockEntity blockEntity = query.getLevel().getBlockEntity(query.getQueryPosition());
        if (!(blockEntity instanceof final FlashDriveBlockEntity drive)) {
            return Invalidatable.empty();
        }

        var device = drive.getDevice();
        if (device == null) {
            device = new FlashDriveDevice(drive);
            drive.setDevice(device);
        }

        return Invalidatable.of(device);
    }
}
