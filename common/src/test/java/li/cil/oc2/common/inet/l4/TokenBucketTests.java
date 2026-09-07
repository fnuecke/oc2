/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenBucketTests {
    @Test
    public void startsFullAndRefillsOverTime() {
        final TokenBucket bucket = new TokenBucket(2, 4);
        final long start = System.nanoTime();

        assertTrue(bucket.tryAcquire(start));
        assertTrue(bucket.tryAcquire(start));
        assertFalse(bucket.tryAcquire(start), "the burst should be spent");

        assertFalse(bucket.tryAcquire(start + TimeUnit.MILLISECONDS.toNanos(200)),
            "a quarter of a second earns one token, not before");
        assertTrue(bucket.tryAcquire(start + TimeUnit.MILLISECONDS.toNanos(250)));
        assertFalse(bucket.tryAcquire(start + TimeUnit.MILLISECONDS.toNanos(250)));

        assertTrue(bucket.tryAcquire(start + TimeUnit.SECONDS.toNanos(10)));
        assertTrue(bucket.tryAcquire(start + TimeUnit.SECONDS.toNanos(10)));
        assertFalse(bucket.tryAcquire(start + TimeUnit.SECONDS.toNanos(10)),
            "idle time must not accumulate beyond the capacity");
    }
}
