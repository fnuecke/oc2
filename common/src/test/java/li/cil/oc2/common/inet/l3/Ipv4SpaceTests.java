/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l3;

import li.cil.oc2.common.inet.AddressParseException;
import li.cil.oc2.common.inet.InetUtils;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class Ipv4SpaceTests {
    private static long ip(final String address) {
        try {
            return InetUtils.toUnsigned(InetUtils.parseIpv4Address(address));
        } catch (final AddressParseException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void singleAddressIsContained() {
        final Ipv4Space space = new Ipv4Space();
        space.add(ip("1.2.3.4"));

        assertTrue(space.contains(ip("1.2.3.4")));
        assertFalse(space.contains(ip("1.2.3.3")));
        assertFalse(space.contains(ip("1.2.3.5")));
        assertEquals(1, space.size());
    }

    @Test
    public void subnetCoversItsWholeBlock() {
        final Ipv4Space space = new Ipv4Space();
        space.addSubnet(ip("10.0.0.0"), 8);

        assertTrue(space.contains(ip("10.0.0.0")));
        assertTrue(space.contains(ip("10.255.255.255")));
        assertFalse(space.contains(ip("9.255.255.255")));
        assertFalse(space.contains(ip("11.0.0.0")));
        assertEquals(1L << 24, space.size());
    }

    @Test
    public void hostPrefixCoversExactlyOneAddress() {
        final Ipv4Space space = new Ipv4Space();
        space.addSubnet(ip("192.0.2.7"), 32);

        assertTrue(space.contains(ip("192.0.2.7")));
        assertFalse(space.contains(ip("192.0.2.6")));
        assertEquals(1, space.size());
    }

    @Test
    public void zeroPrefixCoversEverything() {
        final Ipv4Space space = new Ipv4Space();
        space.addSubnet(ip("0.0.0.0"), 0);

        assertTrue(space.contains(ip("0.0.0.0")));
        assertTrue(space.contains(ip("127.255.255.255")));
        assertTrue(space.contains(ip("128.0.0.0")));
        assertTrue(space.contains(ip("255.255.255.255")));
        assertEquals(1L << 32, space.size());
    }

    @Test
    public void rangeSpanningTheSignedBoundaryIsContiguous() {
        final Ipv4Space space = new Ipv4Space();
        space.add(ip("100.0.0.0"), ip("200.0.0.0"));

        assertTrue(space.contains(ip("100.0.0.0")));
        assertTrue(space.contains(ip("127.255.255.255")));
        assertTrue(space.contains(ip("128.0.0.0")));
        assertTrue(space.contains(ip("200.0.0.0")));
        assertFalse(space.contains(ip("99.255.255.255")));
        assertFalse(space.contains(ip("200.0.0.1")));
        assertEquals(1, space.rangeCount());
    }

    @Test
    public void topOfTheAddressSpaceDoesNotOverflow() {
        final Ipv4Space space = new Ipv4Space();
        space.addSubnet(ip("240.0.0.0"), 4);

        assertTrue(space.contains(ip("255.255.255.255")));
        assertTrue(space.contains(ip("240.0.0.0")));
        assertFalse(space.contains(ip("239.255.255.255")));
    }

    @Test
    public void adjacentRangesMerge() {
        final Ipv4Space space = new Ipv4Space();
        space.add(ip("1.0.0.0"), ip("1.0.0.10"));
        space.add(ip("1.0.0.11"), ip("1.0.0.20"));

        assertEquals(1, space.rangeCount());
        assertEquals(21, space.size());
        assertTrue(space.contains(ip("1.0.0.15")));
    }

    @Test
    public void overlappingRangesMerge() {
        final Ipv4Space space = new Ipv4Space();
        space.add(ip("1.0.0.0"), ip("1.0.0.20"));
        space.add(ip("1.0.0.10"), ip("1.0.0.30"));

        assertEquals(1, space.rangeCount());
        assertEquals(31, space.size());
    }

    @Test
    public void aRangeSwallowingSeveralOthersLeavesOne() {
        final Ipv4Space space = new Ipv4Space();
        space.add(ip("1.0.0.0"), ip("1.0.0.5"));
        space.add(ip("1.0.1.0"), ip("1.0.1.5"));
        space.add(ip("1.0.2.0"), ip("1.0.2.5"));
        assertEquals(3, space.rangeCount());

        space.add(ip("1.0.0.0"), ip("1.0.2.5"));
        assertEquals(1, space.rangeCount());
    }

    @Test
    public void disjointRangesStaySeparate() {
        final Ipv4Space space = new Ipv4Space();
        space.add(ip("1.0.0.0"), ip("1.0.0.10"));
        space.add(ip("1.0.0.12"), ip("1.0.0.20"));

        assertEquals(2, space.rangeCount());
        assertFalse(space.contains(ip("1.0.0.11")));
    }

    @Test
    public void reversedRangeIsAccepted() {
        final Ipv4Space space = new Ipv4Space();
        space.add(ip("1.0.0.20"), ip("1.0.0.10"));

        assertTrue(space.contains(ip("1.0.0.15")));
        assertEquals(11, space.size());
    }

    @Test
    public void emptySpaceContainsNothing() {
        final Ipv4Space space = new Ipv4Space();

        assertTrue(space.isEmpty());
        assertFalse(space.contains(ip("0.0.0.0")));
        assertFalse(space.contains(ip("255.255.255.255")));
    }

    @Test
    public void membershipAgreesWithABruteForceModel() {
        final Random random = new Random(20260905L);

        for (int trial = 0; trial < 200; ++trial) {
            final Ipv4Space space = new Ipv4Space();
            final Set<Long> model = new HashSet<>();

            final long origin = 0xFFFFFF00L;
            for (int i = 0; i < 8; ++i) {
                final long begin = origin + random.nextInt(200);
                final long end = begin + random.nextInt(40);
                space.add(begin, end);
                for (long address = begin; address <= end; ++address) {
                    model.add(address);
                }
            }

            for (long address = origin - 5; address <= origin + 250; ++address) {
                assertEquals(model.contains(address), space.contains(address),
                    "disagreement at " + InetUtils.ipv4AddressToString((int) address));
            }
            assertEquals(model.size(), space.size(), "merged ranges should cover the same addresses");
        }
    }

    @Test
    public void prefixOutOfRangeIsRejected() {
        final Ipv4Space space = new Ipv4Space();

        assertThrows(IllegalArgumentException.class, () -> space.addSubnet(ip("1.2.3.4"), 33));
        assertThrows(IllegalArgumentException.class, () -> space.addSubnet(ip("1.2.3.4"), -1));
    }
}
