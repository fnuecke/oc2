/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

public final class ThrottledLogger {
    private static final String MISSED_SUFFIX = " ({} earlier messages not logged)";

    // --------------------------------------------------------------------- //

    private final Logger logger;
    private final long intervalNanos;
    private final LongSupplier clock;
    private final Map<Object, Budget> budgets = new HashMap<>();

    // --------------------------------------------------------------------- //

    public ThrottledLogger(final Logger logger, final Duration interval) {
        this(logger, interval, System::nanoTime);
    }

    ThrottledLogger(final Logger logger, final Duration interval, final LongSupplier clock) {
        this.logger = logger;
        this.intervalNanos = interval.toNanos();
        this.clock = clock;
    }

    // --------------------------------------------------------------------- //

    public void info(final String message, final Object... args) {
        final int missed = claim(message);
        if (missed < 0) {
            return;
        }
        if (missed > 0) {
            final Object[] extended = new Object[args.length + 1];
            System.arraycopy(args, 0, extended, 0, args.length);
            extended[args.length] = missed;
            logger.info(message + MISSED_SUFFIX, extended);
        } else {
            logger.info(message, args);
        }
    }

    public void error(final String message, final Throwable throwable) {
        final int missed = claim(throwable.getClass());
        if (missed < 0) {
            return;
        }
        if (missed > 0) {
            logger.error(message + MISSED_SUFFIX.replace("{}", Integer.toString(missed)), throwable);
        } else {
            logger.error(message, throwable);
        }
    }

    // --------------------------------------------------------------------- //

    private synchronized int claim(final Object key) {
        final long now = clock.getAsLong();
        final Budget budget = budgets.get(key);
        if (budget == null) {
            budgets.put(key, new Budget(now));
            return 0;
        }
        if (now - budget.lastLoggedTime < intervalNanos) {
            ++budget.missed;
            return -1;
        }
        final int missed = budget.missed;
        budget.lastLoggedTime = now;
        budget.missed = 0;
        return missed;
    }

    // --------------------------------------------------------------------- //

    private static final class Budget {
        private long lastLoggedTime;
        private int missed;

        private Budget(final long lastLoggedTime) {
            this.lastLoggedTime = lastLoggedTime;
        }
    }
}
