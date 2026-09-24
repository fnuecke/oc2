package com.example.blockentity;

import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.neoforge.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

final class Integration {
    static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Device.BLOCK, ExampleMod.COUNTER_BLOCK_ENTITY.get(),
            (blockEntity, side) -> new ObjectDevice(new CounterDevice(blockEntity), "counter"));
    }
}
