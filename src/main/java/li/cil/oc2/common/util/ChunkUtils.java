/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import li.cil.oc2.common.ext.ChunkAccessExt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChunkUtils {
    /**
     * This will mark a chunk unsaved lazily, right before an attempt to save it would be made due
     * to of these events:
     * <ul>
     * <li>Chunk unloaded.</li>
     * <li>Game paused (singleplayer).</li>
     * <li>Save command.</li>
     * <li>Server stopped.</li>
     * </ul>
     * <p>
     * This is intended for things that change every tick, which would lead to saving to NBT every
     * single tick, when setting {@link net.minecraft.world.level.chunk.ChunkAccess#setUnsaved(boolean)}
     * directly.
     * <p>
     * Instead, this sets a flag on the chunk, which, if true, will cause the chunk to be marked as
     * unsaved just before this flag is checked, for the events listed above. I.e. for all cases
     * where an "explicit" save is performed.
     *
     * @param chunkAccess the chunk to set the flag for.
     */
    public static void setLazyUnsaved(final ChunkAccess chunkAccess) {
        if (chunkAccess instanceof ChunkAccessExt ext) {
            ext.setLazyUnsaved();
        }
    }

    /**
     * This will mark a chunk unsaved lazily, right before an attempt to save it would be made due
     * to of these events:
     * <ul>
     * <li>Chunk unloaded.</li>
     * <li>Game paused (singleplayer).</li>
     * <li>Save command.</li>
     * <li>Server stopped.</li>
     * </ul>
     * <p>
     * This is intended for things that change every tick, which would lead to saving to NBT every
     * single tick, when setting {@link net.minecraft.world.level.chunk.ChunkAccess#setUnsaved(boolean)}
     * directly.
     * <p>
     * Instead, this sets a flag on the chunk, which, if true, will cause the chunk to be marked as
     * unsaved just before this flag is checked, for the events listed above. I.e. for all cases
     * where an "explicit" save is performed.
     *
     * @param level    the level containing the chunk.
     * @param blockPos the block position contained in the chunk.
     */
    public static void setLazyUnsaved(final Level level, final BlockPos blockPos) {
        final int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
        final int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        if (level.hasChunk(chunkX, chunkZ)) {
            setLazyUnsaved(level.getChunk(chunkX, chunkZ));
        }
    }

    @SubscribeEvent
    public static void handleChunkUnload(final ChunkEvent.Unload event) {
        if (event.getChunk() instanceof ChunkAccessExt ext) {
            ext.applyAndClearLazyUnsaved();
        }
    }
}
