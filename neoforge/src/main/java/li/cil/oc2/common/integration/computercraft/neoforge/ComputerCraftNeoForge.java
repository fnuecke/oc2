/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.computercraft.neoforge;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.neoforge.CapabilitiesImpl;
import li.cil.oc2.common.integration.ModIntegration;
import li.cil.oc2.common.integration.computercraft.ComputerCraft;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class ComputerCraftNeoForge {
    @SubscribeEvent
    public static void handleRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        ModIntegration.COMPUTERCRAFT.run(() -> {
            event.registerBlockEntity(PeripheralCapability.get(),
                BlockEntities.NETWORK_CONNECTOR.get(),
                ComputerCraft::peripheralFor);

            final var networkInterface = CapabilitiesImpl.block(Capabilities.NETWORK_INTERFACE);
            for (final BlockEntityType<?> type : ComputerCraft.computerTypes()) {
                event.registerBlockEntity(networkInterface, type, ComputerCraft::networkInterfaceFor);
            }
        });
    }
}
