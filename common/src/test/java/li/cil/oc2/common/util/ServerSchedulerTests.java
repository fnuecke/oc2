/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ServerSchedulerTests {
    @Test
    public void anyChunkUnloadListenerSeesEveryChunk() {
        final LevelAccessor level = mock(LevelAccessor.class);
        final List<ChunkPos> seen = new ArrayList<>();
        final Consumer<ChunkPos> listener = seen::add;

        ServerScheduler.subscribeOnAnyChunkUnload(level, listener);
        try {
            ServerScheduler.onChunkUnload(level, new ChunkPos(1, 1));
            ServerScheduler.onChunkUnload(level, new ChunkPos(2, 2));

            assertEquals(List.of(new ChunkPos(1, 1), new ChunkPos(2, 2)), seen);
        } finally {
            ServerScheduler.unsubscribeOnAnyChunkUnload(level, listener);
        }
    }

    @Test
    public void anyChunkUnloadListenerIsScopedToItsLevel() {
        final LevelAccessor level = mock(LevelAccessor.class);
        final LevelAccessor otherLevel = mock(LevelAccessor.class);
        final List<ChunkPos> seen = new ArrayList<>();
        final Consumer<ChunkPos> listener = seen::add;

        ServerScheduler.subscribeOnAnyChunkUnload(level, listener);
        try {
            ServerScheduler.onChunkUnload(otherLevel, new ChunkPos(1, 1));

            assertTrue(seen.isEmpty());
        } finally {
            ServerScheduler.unsubscribeOnAnyChunkUnload(level, listener);
        }
    }

    @Test
    public void anyChunkUnloadListenerMayUnsubscribeItself() {
        final LevelAccessor level = mock(LevelAccessor.class);
        final List<String> seen = new ArrayList<>();
        @SuppressWarnings({"unchecked", "rawtypes"}) final Consumer<ChunkPos>[] selfRemoving = new Consumer[1];
        selfRemoving[0] = chunkPos -> {
            seen.add("self-removing");
            ServerScheduler.unsubscribeOnAnyChunkUnload(level, selfRemoving[0]);
        };
        final Consumer<ChunkPos> other = chunkPos -> seen.add("other");

        ServerScheduler.subscribeOnAnyChunkUnload(level, selfRemoving[0]);
        ServerScheduler.subscribeOnAnyChunkUnload(level, other);
        try {
            assertDoesNotThrow(() -> ServerScheduler.onChunkUnload(level, new ChunkPos(1, 1)));
            assertEquals(2, seen.size(), "both listeners must run");

            // The self-removing one is gone now, the other one is not.
            seen.clear();
            ServerScheduler.onChunkUnload(level, new ChunkPos(1, 1));
            assertEquals(List.of("other"), seen);
        } finally {
            ServerScheduler.unsubscribeOnAnyChunkUnload(level, other);
        }
    }

    @Test
    public void chunkUnloadListenerMayUnsubscribeItself() {
        final LevelAccessor level = mock(LevelAccessor.class);
        final ChunkPos chunkPos = new ChunkPos(3, 4);
        final List<String> seen = new ArrayList<>();
        final Runnable[] selfRemoving = new Runnable[1];
        selfRemoving[0] = () -> {
            seen.add("self-removing");
            ServerScheduler.unsubscribeOnUnload(level, chunkPos, selfRemoving[0]);
        };
        final Runnable other = () -> seen.add("other");

        ServerScheduler.subscribeOnUnload(level, chunkPos, selfRemoving[0]);
        ServerScheduler.subscribeOnUnload(level, chunkPos, other);
        try {
            assertDoesNotThrow(() -> ServerScheduler.onChunkUnload(level, chunkPos));
            assertEquals(2, seen.size(), "both listeners must run");
        } finally {
            ServerScheduler.unsubscribeOnUnload(level, chunkPos, other);
        }
    }
}
