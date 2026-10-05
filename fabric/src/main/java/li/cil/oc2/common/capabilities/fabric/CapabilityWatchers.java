/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.fabric;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import li.cil.oc2.common.capabilities.Capabilities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class CapabilityWatchers {
    // Weak, like NeoForge's listeners: callers keep their handles alive.
    private static final Map<LevelAccessor, Long2ObjectMap<Map<BlockPos, List<WeakReference<Handle>>>>> HANDLES = new WeakHashMap<>();

    // --------------------------------------------------------------------- //

    static void initialize() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) ->
            invalidate(level, blockEntity.getBlockPos()));
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) ->
            invalidate(level, blockEntity.getBlockPos()));

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> invalidate(level, chunk.getPos()));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> invalidate(level, chunk.getPos()));
    }

    static Capabilities.InvalidationHandle listen(final LevelAccessor level, final BlockPos pos, final Runnable callback) {
        final Handle result = new Handle(callback);

        final List<WeakReference<Handle>> handles = HANDLES
            .computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>())
            .computeIfAbsent(ChunkPos.asLong(pos), ignored -> new HashMap<>())
            .computeIfAbsent(pos.immutable(), ignored -> new ArrayList<>());
        handles.removeIf(reference -> {
            final Handle existing = reference.get();
            return existing == null || existing.callback == null;
        });
        handles.add(new WeakReference<>(result));

        return result;
    }

    static void invalidate(final LevelAccessor level, final BlockPos pos) {
        final Long2ObjectMap<Map<BlockPos, List<WeakReference<Handle>>>> byChunk = HANDLES.get(level);
        if (byChunk == null) {
            return;
        }

        final long chunkKey = ChunkPos.asLong(pos);
        final Map<BlockPos, List<WeakReference<Handle>>> byPosition = byChunk.get(chunkKey);
        if (byPosition == null) {
            return;
        }

        final List<WeakReference<Handle>> handles = byPosition.remove(pos);
        if (byPosition.isEmpty()) {
            byChunk.remove(chunkKey);
        }

        if (handles != null) {
            run(handles);
        }
    }

    // --------------------------------------------------------------------- //

    private static void invalidate(final LevelAccessor level, final ChunkPos chunkPos) {
        final Long2ObjectMap<Map<BlockPos, List<WeakReference<Handle>>>> byChunk = HANDLES.get(level);
        if (byChunk == null) {
            return;
        }

        final Map<BlockPos, List<WeakReference<Handle>>> byPosition = byChunk.remove(chunkPos.toLong());
        if (byPosition != null) {
            byPosition.values().forEach(CapabilityWatchers::run);
        }
    }

    private static void run(final List<WeakReference<Handle>> handles) {
        for (final WeakReference<Handle> reference : handles) {
            final Handle handle = reference.get();
            if (handle != null) {
                handle.run();
            }
        }
    }

    private CapabilityWatchers() {
    }

    // --------------------------------------------------------------------- //

    private static final class Handle implements Capabilities.InvalidationHandle {
        @Nullable
        private Runnable callback;

        Handle(final Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void drop() {
            callback = null;
        }

        void run() {
            final Runnable callback = this.callback;
            if (callback != null) {
                drop();
                callback.run();
            }
        }
    }
}
