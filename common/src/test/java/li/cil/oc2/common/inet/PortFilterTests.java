/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import li.cil.oc2.common.Config;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PortFilterTests {
    private static PortFilter denying(final String... rules) {
        return new PortFilter(List.of(rules));
    }

    @Test
    public void withNoRulesEveryPortIsAllowed() {
        final PortFilter filter = denying();

        assertTrue(filter.isAllowed((short) 80));
        assertTrue(filter.isAllowed((short) 25));
        assertTrue(filter.isAllowed((short) 0));
    }

    @Test
    public void singlePortsAreDenied() {
        final PortFilter filter = denying("25");

        assertFalse(filter.isAllowed((short) 25));
        assertTrue(filter.isAllowed((short) 24));
        assertTrue(filter.isAllowed((short) 26));
    }

    @Test
    public void rangesAreDenied() {
        final PortFilter filter = denying("137-139");

        assertTrue(filter.isAllowed((short) 136));
        assertFalse(filter.isAllowed((short) 137));
        assertFalse(filter.isAllowed((short) 138));
        assertFalse(filter.isAllowed((short) 139));
        assertTrue(filter.isAllowed((short) 140));
    }

    @Test
    public void portsAboveTheSignedRangeAreHandled() {
        final PortFilter filter = denying("65535", "40000-40002");

        assertFalse(filter.isAllowed((short) 65535));
        assertFalse(filter.isAllowed((short) 40001));
        assertTrue(filter.isAllowed((short) 39999));
    }

    @Test
    public void anUnenforceableRuleRefusesToStart() {
        assertThrows(IllegalArgumentException.class, () -> denying("70000"));
        assertThrows(IllegalArgumentException.class, () -> denying("-1"));
        assertThrows(IllegalArgumentException.class, () -> denying("not a port"));
        assertThrows(IllegalArgumentException.class, () -> denying("200-100"));
    }

    @Test
    public void blanksAndCommentsAreSkipped() {
        final PortFilter filter = denying("", "   ", "# a comment", "25");

        assertFalse(filter.isAllowed((short) 25));
        assertTrue(filter.isAllowed((short) 80));
    }

    @Test
    public void theShippedDefaultsCloseTheCommonlyAbusedPorts() {
        final PortFilter filter = new PortFilter(Config.internetDeniedPorts);

        for (final int port : new int[]{25, 465, 587, 137, 138, 139, 445, 1900, 3389, 11211}) {
            assertFalse(filter.isAllowed((short) port), "port " + port + " should be closed by default");
        }
        for (final int port : new int[]{80, 443, 53, 22, 8080, 6667}) {
            assertTrue(filter.isAllowed((short) port), "port " + port + " should be reachable by default");
        }
    }
}
