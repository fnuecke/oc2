/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.neoforge;

import dev.architectury.utils.EnvExecutor;
import li.cil.oc2.api.API;
import li.cil.oc2.client.neoforge.ClientSetupNeoForge;
import li.cil.oc2.common.Main;
import net.fabricmc.api.EnvType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(API.MOD_ID)
public final class MainNeoForge {
    public MainNeoForge(final IEventBus modEventBus, final ModContainer modContainer) {
        ModEventBus.INSTANCE = modEventBus;
        ModEventBus.MOD_CONTAINER = modContainer;

        Main.initialize();

        modEventBus.register(CommonSetupNeoForge.class);
        EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> modEventBus.register(ClientSetupNeoForge.class));
    }
}
