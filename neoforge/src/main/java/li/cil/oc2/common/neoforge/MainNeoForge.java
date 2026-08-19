/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.common.Main;
import li.cil.oc2.common.integration.neoforge.IMCNeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(API.MOD_ID)
public final class MainNeoForge {
    public MainNeoForge(final IEventBus modEventBus, final ModContainer modContainer) {
        ModEventBus.INSTANCE = modEventBus;
        ModEventBus.MOD_CONTAINER = modContainer;

        Main.initialize();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Manuals.initialize();
        }
        IMCNeoForge.initialize();
    }
}
