/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class ThrottledLogger {
    private final Logger logger;
    private final long intervalNanos;
    private final Map<Class<?>, Long> lastLogged = new HashMap<>();

    // --------------------------------------------------------------------- //

    public ThrottledLogger(final Logger logger, final Duration interval) {
        this.logger = logger;
        this.intervalNanos = interval.toNanos();
    }

    // --------------------------------------------------------------------- //

    public void error(final String message, final Throwable throwable) {
        if (shouldLog(throwable.getClass())) {
            logger.error(message, throwable);
        }
    }

    // --------------------------------------------------------------------- //

    private synchronized boolean shouldLog(final Class<?> kind) {
        final long now = System.nanoTime();
        final Long last = lastLogged.get(kind);
        if (last != null && now - last < intervalNanos) {
            return false;
        }
        lastLogged.put(kind, now);
        return true;
    }
}
