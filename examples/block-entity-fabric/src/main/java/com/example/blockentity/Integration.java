package com.example.blockentity;

import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.fabric.Lookups;

final class Integration {
    static void registerLookups() {
        Lookups.Device.BLOCK.registerForBlockEntity(
            (blockEntity, side) -> new ObjectDevice(new CounterDevice(blockEntity), "counter"),
            ExampleMod.COUNTER_BLOCK_ENTITY);
    }
}
