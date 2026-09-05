/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.DiskDriveDevice;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class DiskDriveDeviceProvider implements BlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        final Optional<ArchitectureType> architectureType = query.getArchitectureType();
        if (architectureType.isEmpty()) {
            return Invalidatable.empty();
        }

        final BlockEntity blockEntity = query.getLevel().getBlockEntity(query.getQueryPosition());
        if (!(blockEntity instanceof final DiskDriveBlockEntity drive)) {
            return Invalidatable.empty();
        }

        final boolean isBackSide = query.getQuerySide() == drive.getBlockState().getValue(DiskDriveBlock.FACING).getOpposite();
        if (!isBackSide) {
            return Invalidatable.empty();
        }

        var device = drive.getDevice();
        if (device == null || device.getArchitectureType() != architectureType.get()) {
            device = new DiskDriveDevice(drive, architectureType.get());
            drive.setDevice(device);
        }

        return Invalidatable.of(device);
    }
}
