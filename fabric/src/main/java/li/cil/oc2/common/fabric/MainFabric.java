/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fabric;

import li.cil.oc2.api.API;
import li.cil.oc2.api.platform.FabricRegistrationInitializer;
import li.cil.oc2.common.Main;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.capabilities.fabric.CapabilityRegistrationFabric;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.util.fabric.FakePlayerUtilsImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;

public final class MainFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Main.initialize();

        FabricLoader.getInstance()
            .getEntrypoints(API.MOD_ID + ":registration", FabricRegistrationInitializer.class)
            .forEach(FabricRegistrationInitializer::registerObjects);

        Network.initialize();

        CapabilityRegistrationFabric.initialize();
        ChunkEventsFabric.initialize();
        EntityEventsFabric.initialize();
        FakePlayerUtilsImpl.initialize();
        WrenchInteractionFabric.initialize();

        registerBusCablePickBlock();
    }

    // --------------------------------------------------------------------- //

    /**
     * Matches NeoForge's {@code IBlockExtension#getCloneItemStack}.
     */
    private static void registerBusCablePickBlock() {
        PlayerPickItemEvents.BLOCK.register((player, pos, state, includeData) -> {
            final ItemStack stack = BusCableBlock.getPickedStack(player.level(), pos, player);
            return stack.isEmpty() ? null : stack;
        });
    }
}
