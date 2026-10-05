package com.example.thirdpartyblock;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.Optional;

public final class FurnaceDeviceProvider implements BlockDeviceProvider {
    @Override
    public Optional<Device> getDevice(final BlockDeviceQuery query) {
        if (query.getLevel().getBlockEntity(query.getQueryPosition()) instanceof final AbstractFurnaceBlockEntity furnace) {
            return Optional.of(new ObjectDevice(new FurnaceDevice(furnace), "furnace"));
        }
        return Optional.empty();
    }
}
