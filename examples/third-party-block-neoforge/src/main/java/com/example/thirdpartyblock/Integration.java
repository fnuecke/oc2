package com.example.thirdpartyblock;

import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.util.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class Integration {
    private static final DeferredRegister<BlockDeviceProvider> BLOCK_DEVICE_PROVIDERS =
        DeferredRegister.create(Registries.BLOCK_DEVICE_PROVIDER, ExampleMod.MOD_ID);

    static void initialize(final IEventBus modEventBus) {
        BLOCK_DEVICE_PROVIDERS.register("furnace", FurnaceDeviceProvider::new);
        BLOCK_DEVICE_PROVIDERS.register(modEventBus);
    }
}
