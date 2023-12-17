/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import li.cil.oc2.common.mixin.ServerChunkCacheMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChunkUtils {
    /**
     * All chunks marked for lazy saving. The lazy unsaved state will be applied when
     * chunks unload and when chunks get explicitly saved, via the {@link ServerChunkCacheMixin}.
     */
    private static final Set<ChunkAccess> UNSAVED_CHUNKS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

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
        UNSAVED_CHUNKS.add(chunkAccess);
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
    public static void setLazyUnsaved(final LevelAccessor level, final BlockPos blockPos) {
        final int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
        final int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        if (level.hasChunk(chunkX, chunkZ)) {
            setLazyUnsaved(level.getChunk(chunkX, chunkZ));
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
     * @param chunkPos the position of the chunk.
     */
    public static void setLazyUnsaved(final LevelAccessor level, final ChunkPos chunkPos) {
        final int chunkX = chunkPos.x;
        final int chunkZ = chunkPos.z;
        if (level.hasChunk(chunkX, chunkZ)) {
            setLazyUnsaved(level.getChunk(chunkX, chunkZ));
        }
    }

    /**
     * Marks the specified as unsaved, if it was marked as lazy unsaved before
     * and clears the lazy unsaved flags.
     *
     * @param chunk the chunk to apply the lazy unsaved state for.
     */
    public static void applyChunkLazyUnsaved() {
        for (final ChunkAccess chunk : UNSAVED_CHUNKS) {
            chunk.setUnsaved(true);
        }
        UNSAVED_CHUNKS.clear();
    }

    @SubscribeEvent
    public static void handleChunkUnload(final ChunkEvent.Unload event) {
        final ChunkAccess chunk = event.getChunk();
        if (UNSAVED_CHUNKS.remove(chunk)) {
            chunk.setUnsaved(true);
        }
    }
}
