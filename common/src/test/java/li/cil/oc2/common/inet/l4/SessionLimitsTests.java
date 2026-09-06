/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessionLimitsTests {
    @Test
    public void aLoneGatewayGetsTheFullPerGatewayCap() {
        final SessionLimits limits = new SessionLimits(16, 128);

        assertEquals(16, limits.shareFor(1));
        assertEquals(16, limits.shareFor(0));
    }

    @Test
    public void theShareShrinksAsGatewaysAppear() {
        final SessionLimits limits = new SessionLimits(16, 128);

        assertEquals(16, limits.shareFor(8));
        assertEquals(12, limits.shareFor(10));
        assertEquals(2, limits.shareFor(64));
    }

    @Test
    public void everyGatewayKeepsAtLeastOneSession() {
        final SessionLimits limits = new SessionLimits(16, 128);

        assertEquals(1, limits.shareFor(128));
        assertEquals(1, limits.shareFor(1000));
    }

    @Test
    public void oneOperatorCannotStarveTheRest() {
        final SessionLimits limits = new SessionLimits(16, 128);
        final int gateways = 9; // Eight built by an attacker, one belonging to somebody else.

        int taken = 0;
        for (int gateway = 0; gateway < 8; ++gateway) {
            int held = 0;
            while (limits.tryAcquire(held, gateways)) {
                ++held;
                ++taken;
            }
        }

        assertTrue(taken < 128, "the attacker should not be able to take the whole budget");
        assertTrue(limits.tryAcquire(0, gateways),
            "a gateway that did not race for sessions must still be able to open one");
    }

    @Test
    public void theGlobalCapStillBinds() {
        final SessionLimits limits = new SessionLimits(16, 4);

        assertTrue(limits.tryAcquire(0, 1));
        assertTrue(limits.tryAcquire(1, 1));
        assertTrue(limits.tryAcquire(2, 1));
        assertTrue(limits.tryAcquire(3, 1));
        assertFalse(limits.tryAcquire(4, 1), "the server-wide cap must still apply");
    }

    @Test
    public void releasingReturnsCapacity() {
        final SessionLimits limits = new SessionLimits(16, 1);

        assertTrue(limits.tryAcquire(0, 1));
        assertFalse(limits.tryAcquire(0, 1));
        limits.release();
        assertTrue(limits.tryAcquire(0, 1));
    }
}
