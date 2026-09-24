package com.example.thirdpartyblock;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public final class FurnaceDeviceProvider implements BlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        if (query.getLevel().getBlockEntity(query.getQueryPosition()) instanceof final AbstractFurnaceBlockEntity furnace) {
            return Invalidatable.of(new ObjectDevice(new FurnaceDevice(furnace), "furnace"));
        }
        return Invalidatable.empty();
    }
}
