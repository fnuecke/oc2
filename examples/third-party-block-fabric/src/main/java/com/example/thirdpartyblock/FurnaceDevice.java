package com.example.thirdpartyblock;

import li.cil.oc2.api.bus.device.object.Callback;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

// This being a record means equals() matches devices from repeated queries for the same
// block entity; otherwise the bus would remove and re-add the device on every scan.
public record FurnaceDevice(AbstractFurnaceBlockEntity furnace) {
    @Callback(description = "Returns whether the furnace is burning fuel.")
    public boolean isLit() {
        return furnace.getBlockState().getValue(AbstractFurnaceBlock.LIT);
    }

    @Callback(description = "Returns the direction the furnace faces.")
    public Direction getFacing() {
        return furnace.getBlockState().getValue(AbstractFurnaceBlock.FACING);
    }
}
