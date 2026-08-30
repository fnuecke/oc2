/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

public final class ServerUtils {
    @Nullable
    private static volatile MinecraftServer serverInstance;
    @Nullable
    private static volatile Thread serverThread;

    // --------------------------------------------------------------------- //

    public static void initialize() {
        LifecycleEvent.SERVER_BEFORE_START.register(value -> {
            serverInstance = value;
            serverThread = value.getRunningThread();
        });
        LifecycleEvent.SERVER_STOPPED.register(value -> {
            serverInstance = null;
            // Don't clear thread for isOnServerThread correctness.
        });
    }

    public static boolean isOnServerThread() {
        final Thread thread = serverThread;
        return thread == null || thread == Thread.currentThread();
    }

    public static HolderLookup.Provider getRegistryAccess() {
        final MinecraftServer server = serverInstance;
        return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
    }

    private ServerUtils() {
    }
}
