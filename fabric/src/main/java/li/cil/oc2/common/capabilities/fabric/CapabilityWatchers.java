/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.fabric;

import li.cil.oc2.api.util.Invalidatable;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class CapabilityWatchers {
    private static final Map<LevelAccessor, Map<BlockPos, List<Invalidatable<?>>>> WATCHERS = new WeakHashMap<>();
    private static final Set<InvalidationKey> INVALIDATING = new HashSet<>();

    // ------------------------------------------------------------- //

    static void initialize() {
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) ->
                invalidate(level, blockEntity.getBlockPos()));

        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> invalidate(level, chunk.getPos()));
    }

    static <T> Invalidatable<T> watch(final LevelAccessor level, final BlockPos pos, final T value) {
        final Invalidatable<T> result = Invalidatable.of(value);

        final BlockPos key = pos.immutable();
        final List<Invalidatable<?>> watchers = WATCHERS
                .computeIfAbsent(level, ignored -> new HashMap<>())
                .computeIfAbsent(key, ignored -> new ArrayList<>());
        watchers.add(result);

        result.addListener(ignored -> remove(level, key, result));

        return result;
    }

    static void invalidate(final LevelAccessor level, final BlockPos pos) {
        final Map<BlockPos, List<Invalidatable<?>>> byPosition = WATCHERS.get(level);
        if (byPosition == null) {
            return;
        }

        final List<Invalidatable<?>> watchers = byPosition.remove(pos);
        if (watchers == null) {
            return;
        }

        final InvalidationKey key = new InvalidationKey(level, pos.immutable());
        if (!INVALIDATING.add(key)) {
            return;
        }

        try {
            // Copy: invalidating fires the listener that removes the entry we are iterating.
            for (final Invalidatable<?> watcher : new ArrayList<>(watchers)) {
                watcher.invalidate();
            }
        } finally {
            INVALIDATING.remove(key);
        }

        if (byPosition.isEmpty()) {
            WATCHERS.remove(level);
        }
    }

    private record InvalidationKey(LevelAccessor level, BlockPos pos) {
    }

    // ------------------------------------------------------------- //

    private static void invalidate(final LevelAccessor level, final ChunkPos chunkPos) {
        final Map<BlockPos, List<Invalidatable<?>>> byPosition = WATCHERS.get(level);
        if (byPosition == null) {
            return;
        }

        final List<BlockPos> positions = byPosition.keySet().stream()
                .filter(pos -> new ChunkPos(pos).equals(chunkPos))
                .toList();
        for (final BlockPos pos : positions) {
            invalidate(level, pos);
        }
    }

    private static void remove(final LevelAccessor level, final BlockPos pos, final Invalidatable<?> watcher) {
        final Map<BlockPos, List<Invalidatable<?>>> byPosition = WATCHERS.get(level);
        if (byPosition == null) {
            return;
        }

        final List<Invalidatable<?>> watchers = byPosition.get(pos);
        if (watchers == null) {
            return;
        }

        watchers.remove(watcher);
        if (watchers.isEmpty()) {
            byPosition.remove(pos);
            if (byPosition.isEmpty()) {
                WATCHERS.remove(level);
            }
        }
    }

    private CapabilityWatchers() {
    }
}
