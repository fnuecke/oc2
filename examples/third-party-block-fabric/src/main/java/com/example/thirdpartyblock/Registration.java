package com.example.thirdpartyblock;

import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.platform.FabricRegistrationInitializer;
import li.cil.oc2.api.util.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

// Fabric only calls this when oc2 is present.
public final class Registration implements FabricRegistrationInitializer {
    public static final String MOD_ID = "oc2_example_third_party_block";

    @Override
    @SuppressWarnings("unchecked")
    public void registerObjects() {
        final Registry<BlockDeviceProvider> registry =
            (Registry<BlockDeviceProvider>) BuiltInRegistries.REGISTRY.get(Registries.BLOCK_DEVICE_PROVIDER.location());
        Registry.register(registry, ResourceLocation.fromNamespaceAndPath(MOD_ID, "furnace"), new FurnaceDeviceProvider());
    }
}
