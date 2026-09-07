/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.socket;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class ReachabilityProbe {
    private static final int[] PROBE_PORTS = {443, 80};
    private static final long RESULT_LIFETIME_NANOS = TimeUnit.SECONDS.toNanos(30);
    private static final int MAX_CACHED_ADDRESSES = 256;

    // --------------------------------------------------------------------- //

    private final Executor executor;
    private final int timeoutMs;
    private final Map<InetAddress, Result> results = new LinkedHashMap<>();

    // --------------------------------------------------------------------- //

    public ReachabilityProbe(final Executor executor, final int timeoutMs) {
        this.executor = executor;
        this.timeoutMs = timeoutMs;
    }

    // --------------------------------------------------------------------- //

    public void probe(final InetAddress address, final Runnable onReachable) {
        final Boolean known = claim(address);
        if (known != null) {
            if (known) {
                onReachable.run();
            }
            return;
        }

        try {
            executor.execute(() -> {
                final boolean reachable = connect(address);
                complete(address, reachable);
                if (reachable) {
                    onReachable.run();
                }
            });
        } catch (final RejectedExecutionException e) {
            // Too busy, drop it; address would stay pending and unanswerable until evicted otherwise.
            synchronized (results) {
                results.remove(address);
            }
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private Boolean claim(final InetAddress address) {
        synchronized (results) {
            final Result result = results.get(address);
            if (result != null) {
                if (result.pending) {
                    return Boolean.FALSE;
                }
                if (System.nanoTime() - result.time < RESULT_LIFETIME_NANOS) {
                    return result.reachable;
                }
                result.pending = true;
                return null;
            }

            if (results.size() >= MAX_CACHED_ADDRESSES) {
                final Iterator<Map.Entry<InetAddress, Result>> iterator = results.entrySet().iterator();
                iterator.next();
                iterator.remove();
            }
            results.put(address, new Result());
            return null;
        }
    }

    private void complete(final InetAddress address, final boolean reachable) {
        synchronized (results) {
            final Result result = results.get(address);
            if (result == null) {
                // Evicted while the probe was running.
                return;
            }
            result.pending = false;
            result.reachable = reachable;
            result.time = System.nanoTime();
        }
    }

    private boolean connect(final InetAddress address) {
        for (final int port : PROBE_PORTS) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address, port), timeoutMs);
                return true;
            } catch (final ConnectException e) {
                // Refused, so something there answered, which is all we wanted to know. Our own
                // timeout fires long before the system's, so this is never a timeout in disguise.
                return true;
            } catch (final IOException e) {
                // Timed out or unroutable; the next port may still answer.
            }
        }
        return false;
    }

    // --------------------------------------------------------------------- //

    private static final class Result {
        private boolean pending = true;
        private boolean reachable;
        private long time;
    }
}
