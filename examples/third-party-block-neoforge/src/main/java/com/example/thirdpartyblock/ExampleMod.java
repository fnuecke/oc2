package com.example.thirdpartyblock;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = "oc2_example_third_party_block";

    public ExampleMod(final IEventBus modEventBus) {
        if (ModList.get().isLoaded("oc2")) {
            Integration.initialize(modEventBus);
        }
    }
}
