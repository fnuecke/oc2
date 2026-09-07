/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ThrottledLoggerTests {
    @Test
    public void droppedLinesAreCountedOnTheNextOneThatPasses() {
        final Logger logger = mock(Logger.class);
        final List<Object[]> lines = new ArrayList<>();
        doAnswer(invocation -> {
            lines.add(invocation.getArguments());
            return null;
        }).when(logger).info(anyString(), any(Object[].class));

        final long[] now = {0};
        final ThrottledLogger throttled = new ThrottledLogger(logger, Duration.ofSeconds(1), () -> now[0]);

        throttled.info("Opened {}.", "a");
        throttled.info("Opened {}.", "b");
        throttled.info("Opened {}.", "c");
        throttled.info("Other {}.", "x");

        now[0] = TimeUnit.SECONDS.toNanos(1);
        throttled.info("Opened {}.", "d");
        throttled.info("Other {}.", "y");

        // Mockito hands varargs over flattened: message first, then each argument.
        assertEquals(4, lines.size(), "one line per key per second");
        assertEquals(List.of("Opened {}.", "a"), Arrays.asList(lines.get(0)));
        assertEquals(List.of("Other {}.", "x"), Arrays.asList(lines.get(1)));
        assertEquals(List.of("Opened {}. ({} earlier messages not logged)", "d", 2), Arrays.asList(lines.get(2)));
        assertEquals(List.of("Other {}.", "y"), Arrays.asList(lines.get(3)), "a key without drops gets no suffix");
    }

    @Test
    public void errorsAreKeyedByThrowableClass() {
        final Logger logger = mock(Logger.class);
        final long[] now = {0};
        final ThrottledLogger throttled = new ThrottledLogger(logger, Duration.ofSeconds(1), () -> now[0]);

        throttled.error("Failed.", new IllegalStateException());
        throttled.error("Failed.", new IllegalStateException());
        throttled.error("Failed.", new IllegalArgumentException());

        verify(logger, times(1)).error(eq("Failed."), any(IllegalStateException.class));
        verify(logger, times(1)).error(eq("Failed."), any(IllegalArgumentException.class));

        now[0] = TimeUnit.SECONDS.toNanos(1);
        throttled.error("Failed.", new IllegalStateException());
        verify(logger).error(eq("Failed. (1 earlier messages not logged)"), any(IllegalStateException.class));
    }
}
