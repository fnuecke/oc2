/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class ClientLevelEventsNeoForge {
    @SubscribeEvent
    public static void handleChunkUnload(final ChunkEvent.Unload event) {
        final LevelAccessor level = event.getLevel();
        if (!level.isClientSide()) {
            return;
        }

        NetworkCableRenderer.onChunkUnload(event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void handleLevelUnload(final LevelEvent.Unload event) {
        final LevelAccessor level = event.getLevel();
        if (!level.isClientSide()) {
            return;
        }

        NetworkCableRenderer.onLevelUnload(level);
    }

    private ClientLevelEventsNeoForge() {
    }
}
