/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.fabric;

import li.cil.oc2.client.renderer.NetworkCableRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.multiplayer.ClientLevel;

import javax.annotation.Nullable;
import java.util.Objects;

public final class ClientLevelEventsFabric {
    @Nullable
    private static ClientLevel lastLevel; // catch dimension changes

    public static void initialize() {
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            if (lastLevel != null && !Objects.equals(lastLevel, level)) {
                NetworkCableRenderer.onLevelUnload(lastLevel);
            }
            lastLevel = level;
        });

        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
            NetworkCableRenderer.onChunkUnload(chunk.getPos()));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (lastLevel != null) {
                NetworkCableRenderer.onLevelUnload(lastLevel);
                lastLevel = null;
            }
        });
    }

    private ClientLevelEventsFabric() {
    }
}
