/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ServerSchedulerTests {
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
