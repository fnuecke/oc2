/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

public final class ServerUtils {
    @Nullable private static MinecraftServer server;

    // ------------------------------------------------------------- //

    public static void initialize() {
        LifecycleEvent.SERVER_BEFORE_START.register(value -> server = value);
        LifecycleEvent.SERVER_STOPPED.register(value -> server = null);
    }

    public static HolderLookup.Provider getRegistryAccess() {
        return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
    }

    private ServerUtils() {
    }
}
