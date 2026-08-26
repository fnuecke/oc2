/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fabric;

import li.cil.oc2.api.API;
import li.cil.oc2.api.platform.FabricRegistrationInitializer;
import li.cil.oc2.common.Main;
import li.cil.oc2.common.capabilities.fabric.CapabilityRegistrationFabric;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.util.fabric.FakePlayerUtilsImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

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
        FakePlayerUtilsImpl.initialize();
        WrenchInteractionFabric.initialize();
    }
}
