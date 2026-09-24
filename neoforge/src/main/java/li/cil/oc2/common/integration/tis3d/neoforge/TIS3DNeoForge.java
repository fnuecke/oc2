/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.tis3d.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.neoforge.CapabilitiesImpl;
import li.cil.oc2.common.integration.ModIntegration;
import li.cil.oc2.common.integration.tis3d.TIS3D;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class TIS3DNeoForge {
    @SubscribeEvent
    public static void handleConstructMod(final FMLConstructModEvent event) {
        ModIntegration.TIS3D.run(TIS3D::registerProviders);
    }

    @SubscribeEvent
    public static void handleRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        ModIntegration.TIS3D.run(() -> event.registerBlockEntity(
            CapabilitiesImpl.block(Capabilities.NETWORK_INTERFACE),
            TIS3D.casingBlockEntityType(),
            TIS3D::networkInterfaceFor));
    }

    private TIS3DNeoForge() {
    }
}
