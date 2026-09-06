/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.socket;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReachabilityProbeTests {
    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    // --------------------------------------------------------------------- //

    @Test
    public void aRepeatedProbeIsAnsweredFromTheCache() {
        final CountingExecutor executor = new CountingExecutor();
        final ReachabilityProbe probe = new ReachabilityProbe(executor, 500);

        final AtomicInteger answered = new AtomicInteger();
        probe.probe(LOOPBACK, answered::incrementAndGet);
        assertEquals(1, executor.count, "the first request should connect");
        assertEquals(1, answered.get(), "loopback is up, so the echo request should be answered");

        for (int i = 0; i < 8; ++i) {
            probe.probe(LOOPBACK, answered::incrementAndGet);
        }
        assertEquals(1, executor.count, "a cached address should not be connected to again");
        assertEquals(9, answered.get(), "every echo request should still be answered");
    }

    @Test
    public void anAddressIsNotProbedTwiceAtOnce() {
        final DeferredExecutor executor = new DeferredExecutor();
        final ReachabilityProbe probe = new ReachabilityProbe(executor, 500);

        final AtomicInteger answered = new AtomicInteger();
        probe.probe(LOOPBACK, answered::incrementAndGet);
        probe.probe(LOOPBACK, answered::incrementAndGet);
        assertEquals(1, executor.pending.size(), "the second request should not start a probe");
        assertEquals(0, answered.get(), "nothing is known until the probe finishes");

        executor.runAll();
        assertEquals(1, answered.get(), "only the request that started the probe gets a reply");
    }

    // --------------------------------------------------------------------- //

    private static final class CountingExecutor implements Executor {
        private int count;

        @Override
        public void execute(final Runnable command) {
            ++count;
            command.run();
        }
    }

    private static final class DeferredExecutor implements Executor {
        private final List<Runnable> pending = new ArrayList<>();

        @Override
        public void execute(final Runnable command) {
            pending.add(command);
        }

        void runAll() {
            final List<Runnable> commands = List.copyOf(pending);
            pending.clear();
            commands.forEach(Runnable::run);
        }
    }
}
