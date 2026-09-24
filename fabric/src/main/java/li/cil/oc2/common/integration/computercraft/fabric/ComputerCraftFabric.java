/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.computercraft.fabric;

import dan200.computercraft.api.peripheral.PeripheralLookup;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.fabric.CapabilitiesImpl;
import li.cil.oc2.common.integration.ModIntegration;
import li.cil.oc2.common.integration.computercraft.ComputerCraft;
import net.fabricmc.api.ModInitializer;

public final class ComputerCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModIntegration.COMPUTERCRAFT.run(() -> {
            PeripheralLookup.get().registerForBlockEntity(
                ComputerCraft::peripheralFor,
                BlockEntities.NETWORK_CONNECTOR.get());

            final var networkInterface = CapabilitiesImpl.block(Capabilities.NETWORK_INTERFACE);
            networkInterface.registerFallback((level, pos, state, blockEntity, side) ->
                blockEntity != null && ComputerCraft.computerTypes().contains(blockEntity.getType()) ?
                    ComputerCraft.networkInterfaceFor(blockEntity, side) : null);
        });
    }
}
