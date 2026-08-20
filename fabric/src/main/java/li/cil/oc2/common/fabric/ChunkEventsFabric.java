/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fabric;

import li.cil.oc2.common.util.ChunkUtils;
import li.cil.oc2.common.util.ServerScheduler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

public final class ChunkEventsFabric {
    public static void initialize() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                ServerScheduler.onChunkLoad(level, chunk.getPos()));

        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            ServerScheduler.onChunkUnload(level, chunk.getPos());
            ChunkUtils.onChunkUnload(chunk);
        });
    }

    private ChunkEventsFabric() {
    }
}
