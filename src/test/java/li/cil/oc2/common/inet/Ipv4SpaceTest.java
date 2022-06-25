package li.cil.oc2.common.inet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Ipv4SpaceTest {
    @Test
    public void someRangesAndSubnetsTest() {
        final Ipv4Space ipv4Space = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
        ipv4Space.put("127.0.1.1");
        ipv4Space.put("one.one.one.one");
        ipv4Space.put("127.0.0.1/24");
        ipv4Space.put("10.0.0.0/30");
        ipv4Space.put("172.17.0.0/16");
        ipv4Space.put("192.168.30.1-192.168.30.20");
        final String expected = "[" +
                                "172.17.0.0-172.17.255.255, " +
                                "192.168.30.1-192.168.30.20, " +
                                "1.0.0.1, 1.1.1.1, 10.0.0.0-10.0.0.3, " +
                                "127.0.0.0-127.0.0.255, 127.0.1.1" +
                                "]";
        assertEquals(expected, ipv4Space.toString());

        assertTrue(ipv4Space.isAllowed(InetUtils.parseIpv4Address("127.0.0.1")));
        assertTrue(ipv4Space.isAllowed(InetUtils.parseIpv4Address("1.0.0.1")));
        assertTrue(ipv4Space.isAllowed(InetUtils.parseIpv4Address("192.168.30.10")));
        assertFalse(ipv4Space.isAllowed(InetUtils.parseIpv4Address("192.168.30.21")));
    }

    @Test
    public void computeIpSpaceTest() {
        final Ipv4Space space = InetUtils.computeIpSpace("127.0.0.0/8, 10.0.0.0/8, 100.64.0.0/10, 172.16.0.0/12, 192.168.0.0/16, 224.0.0.0/4", "  ");
        assertEquals("[172.16.0.0-172.31.255.255, 192.168.0.0-192.168.255.255, 224.0.0.0-239.255.255.255, 10.0.0.0-10.255.255.255, 100.64.0.0-100.127.255.255, 127.0.0.0-127.255.255.255]", space.toString());
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("192.168.1.1")));
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("1.1.1.1")));
    }
}
